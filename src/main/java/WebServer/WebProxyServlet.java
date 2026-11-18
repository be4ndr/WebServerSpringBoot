package WebServer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.ee11.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;

import java.net.URI;

public class WebProxyServlet extends AsyncMiddleManServlet {

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        URI target = UrlUtils.httpUri(clientRequest.getParameter("url"));
        if (target == null) {
            return null;
        }
        int port = target.getPort();
        if (port == -1) {
            port = "https".equalsIgnoreCase(target.getScheme()) ? 443 : 80;
        }
        return validateDestination(target.getHost(), port) ? target.toASCIIString() : null;
    }

    @Override
    protected HttpField filterServerResponseHeader(HttpServletRequest request, Response response, HttpField field) {
        if (field.getHeader() == HttpHeader.LOCATION) {
            String baseUri = request.getRequestURI() + "?url=";
            return new HttpField(HttpHeader.LOCATION, UrlUtils.modifyURI(
                    baseUri, request.getParameter("url"), field.getValue()));
        }
        return field;
    }

    @Override
    protected ContentTransformer newServerResponseContentTransformer(HttpServletRequest clientRequest,
            HttpServletResponse proxyResponse, Response serverResponse) {
        if (!PageContentTransformer.canTransform(serverResponse)) {
            return ContentTransformer.IDENTITY;
        }
        ContentTransformer transformer = new PageContentTransformer(clientRequest, proxyResponse, serverResponse);
        if ("gzip".equalsIgnoreCase(serverResponse.getHeaders().get(HttpHeader.CONTENT_ENCODING))) {
            return new GZIPContentTransformer(transformer);
        }
        return transformer;
    }
}
