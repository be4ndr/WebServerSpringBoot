package WebServer;

import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.ee11.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.http.HttpHeader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PageContentTransformer implements AsyncMiddleManServlet.ContentTransformer {
    private static final int MAX_TRANSFORM_BYTES = 4 * 1024 * 1024;

    private final HttpServletRequest clientRequest;
    private final HttpServletResponse proxyResponse;
    private final Response serverResponse;
    private ByteArrayOutputStream bufferedContent = new ByteArrayOutputStream();

    public PageContentTransformer(HttpServletRequest clientRequest, HttpServletResponse proxyResponse,
            Response serverResponse) {
        this.clientRequest = clientRequest;
        this.proxyResponse = proxyResponse;
        this.serverResponse = serverResponse;
    }

    static boolean canTransform(Response response) {
        List<String> encodings = response.getHeaders().getValuesList(HttpHeader.CONTENT_ENCODING);
        if (response.getStatus() != HttpServletResponse.SC_OK || contentType(response) == null) {
            return false;
        }
        if (encodings.size() == 1 && "gzip".equalsIgnoreCase(encodings.getFirst())) {
            return true;
        }
        return (encodings.isEmpty() || (encodings.size() == 1 && "identity".equalsIgnoreCase(encodings.getFirst())))
                && response.getHeaders().getLongField(HttpHeader.CONTENT_LENGTH) <= MAX_TRANSFORM_BYTES;
    }

    private boolean isGzip() {
        return "gzip".equalsIgnoreCase(serverResponse.getHeaders().get(HttpHeader.CONTENT_ENCODING));
    }

    private void removeRepresentationHeaders() {
        // These headers describe the upstream bytes and cannot be reused after rewriting or recompression.
        for (String header : List.of("ETag", "Content-Range", "Content-MD5", "Digest", "Content-Digest", "Repr-Digest")) {
            proxyResponse.setHeader(header, null);
        }
    }

    private static MediaType contentType(Response response) {
        String header = response.getHeaders().get(HttpHeader.CONTENT_TYPE);
        if (header == null) {
            return null;
        }
        try {
            MediaType type = MediaType.parseMediaType(header);
            return MediaType.TEXT_HTML.equalsTypeAndSubtype(type)
                    || "text/css".equalsIgnoreCase(type.getType() + "/" + type.getSubtype()) ? type : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
        if (bufferedContent != null && input.remaining() > MAX_TRANSFORM_BYTES - bufferedContent.size()) {
            if (isGzip()) {
                removeRepresentationHeaders();
            }
            output.add(ByteBuffer.wrap(bufferedContent.toByteArray()));
            bufferedContent = null;
        }
        if (bufferedContent == null) {
            output.add(input);
            return;
        }

        byte[] chunk = new byte[input.remaining()];
        input.get(chunk);
        bufferedContent.writeBytes(chunk);
        if (finished) {
            byte[] original = bufferedContent.toByteArray();
            bufferedContent = null;
            MediaType type = contentType(serverResponse);
            Charset charset = type.getCharset();
            String transformed;
            if (MediaType.TEXT_HTML.equalsTypeAndSubtype(type)) {
                Document doc = Jsoup.parse(new ByteArrayInputStream(original),
                        charset == null ? null : charset.name(), "");
                charset = doc.charset();
                transformed = transformHtml(doc);
            } else {
                if (charset == null) {
                    charset = StandardCharsets.UTF_8;
                }
                transformed = transformCss(new String(original, charset));
            }
            if (transformed != null) {
                proxyResponse.setContentType(new MediaType(type, charset).toString());
                output.add(ByteBuffer.wrap(transformed.getBytes(charset)));
            } else {
                output.add(ByteBuffer.wrap(original));
            }
            if (transformed != null || isGzip()) {
                removeRepresentationHeaders();
            }
        }
    }

    private String transformCss(String input) {
        CascadingStyleSheet css = CSSReader.readFromString(input);
        if (css == null) {
            return null;
        }
        CSSVisitor.visitCSSUrl(css, new CSSUrlVisitor(proxyPrefix(), clientRequest.getParameter("url")));
        return new CSSWriter(new CSSWriterSettings().setOptimizedOutput(true)).getCSSAsString(css);
    }

    private String transformHtml(Document doc) {
        modifyElements(doc, "a[href]", "href");
        modifyElements(doc, "link[href]", "href");
        modifyElements(doc, "img[src]", "src");
        modifyElements(doc, "iframe[src]", "src");
        return doc.toString();
    }

    protected void modifyElements(Document doc, String cssQuery, String attributeKey) {
        String baseUri = proxyPrefix();
        String target = clientRequest.getParameter("url");
        for (Element element : doc.select(cssQuery)) {
            element.attr(attributeKey, UrlUtils.modifyURI(baseUri, target, element.attr(attributeKey)));
        }
    }

    private String proxyPrefix() {
        return clientRequest.getRequestURI() + "?url=";
    }
}
