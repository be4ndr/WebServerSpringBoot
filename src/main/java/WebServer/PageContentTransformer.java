package WebServer;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.*;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageContentTransformer implements AsyncMiddleManServlet.ContentTransformer {
    private final AtomicInteger total = new AtomicInteger();
    private final List<ByteBuffer> buffers = new ArrayList<>();
    private final HttpServletRequest clientRequest;
    private final HttpServletResponse proxyResponse;
    private final Response serverResponse;
    private CSSWriter cssWriter = new CSSWriter(new CSSWriterSettings().setOptimizedOutput(true));

    public PageContentTransformer(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {

        this.clientRequest = clientRequest;
        this.proxyResponse = proxyResponse;
        this.serverResponse = serverResponse;
    }

    public void transform(ByteBuffer input, boolean finished, List<ByteBuffer> output) throws IOException {
        ByteBuffer copy = ByteBuffer.allocate(input.remaining());
        copy.put(input).flip();
        buffers.add(copy);
        total.addAndGet(copy.remaining());
        if (finished) {
            ByteBuffer complete = ByteBuffer.allocate(total.get());
            for (ByteBuffer buffer : buffers) {
                complete.put(buffer);
            }
            complete.flip();
            String converted = byteBufferToString(complete);
/*            ByteBuffer replaced = transformHtml(converted);
            output.add(replaced);*/
            ByteBuffer replaced = null;
            if (isHtml()) {
                replaced = transformHtml(converted);
            } else if (isCss()) {
                replaced = transformCss(converted);
            } else {
                replaced = complete;
            }
            output.add(replaced);
        }
    }

    ByteBuffer transformCss(String input) throws IOException {
        String baseUri = clientRequest.getRequestURL() + "?url=";
        String origSiteName = clientRequest.getQueryString();
        String originUri = "";
        Pattern p = Pattern.compile("(http|https)://+[a-z,0-9]*\\.[a-z]*/");
        Matcher m = p.matcher(origSiteName);
        if (m.find()) {
            originUri = origSiteName.substring(m.start(), m.end());
        }
        CascadingStyleSheet css = createCssModel(input);
        CSSVisitor.visitCSSUrl(css, new CSSUrlVisitor(baseUri, originUri));
        String cssString = cssWriter.getCSSAsString(css);
        return stringToByteBuffer(cssString);
    }

/*    CascadingStyleSheet modifyCss (CascadingStyleSheet css) {
        WebServer.CSSUrlVisitor urlVisitor;


    }*/


    private CascadingStyleSheet createCssModel(String cssText) {
        CascadingStyleSheet css = CSSReader.readFromString(cssText, ECSSVersion.LATEST);
        return css;
    }


    private boolean isCss() {
        return serverResponse.getHeaders().get(HttpHeader.CONTENT_TYPE).contains("css");
    }

    private boolean isHtml() {
        return serverResponse.getHeaders().get(HttpHeader.CONTENT_TYPE).contains("html");
    }

    ByteBuffer transformHtml(String input) throws IOException {
        Document doc = createDomModel(input);
        modifyElements(doc, "a[href]", "href");
        modifyElements(doc, "link[href]", "href");
        modifyElements(doc, "img[src]", "src");
        modifyElements(doc, "svg[xmlns]", "xmlns");
        modifyElements(doc, "iframe[src]", "src");
        String htmlString = doc.toString();
        return stringToByteBuffer(htmlString);
    }

    protected void modifyElements(Document doc, String cssQuery, String attributeKey) {
        String baseUri = clientRequest.getRequestURL() + "?url=";
        String siteName = clientRequest.getQueryString();
        Elements query = doc.select(cssQuery);
        siteName = siteName.replace("url=", "");
        for (Element link : query.toArray(new Element[0])) {
            String attributeValue = link.attr(attributeKey).toLowerCase();
            link.attr(attributeKey, UrlUtils.modifyURI(baseUri, siteName, attributeValue));
        }
    }


    protected String extractURI(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        if (request.getQueryString() != null) {
            requestURL.append("?").append(request.getQueryString());
        }
        return requestURL.toString();
    }

    private Document createDomModel(String converted) {
        return Jsoup.parse(converted);
    }

    protected String byteBufferToString(ByteBuffer input) throws IOException {
        String converted = new String(input.array(), proxyResponse.getCharacterEncoding());
        return converted;
    }

    protected ByteBuffer stringToByteBuffer(String msg) {
        ByteBuffer bb = null;
        try {
            bb = ByteBuffer.wrap(msg.getBytes(proxyResponse.getCharacterEncoding()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return bb;
    }

}