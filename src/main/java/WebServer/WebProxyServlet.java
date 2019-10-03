package WebServer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.proxy.AsyncMiddleManServlet;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WebProxyServlet extends AsyncMiddleManServlet {

    private SslContextFactory contextFactory;

    @Override
    protected String rewriteTarget(HttpServletRequest clientRequest) {
        return clientRequest.getParameter("url");
    }

    @Override
    protected HttpClient createHttpClient() throws ServletException {
        return super.createHttpClient();
    }

    @Override
    protected HttpClient newHttpClient() {
        return new HttpClient(contextFactory);
    }

    @Override
    public void init() throws ServletException {
        contextFactory = new SslContextFactory(true);
        super.init();
    }

    @Override
    protected ContentTransformer newServerResponseContentTransformer(HttpServletRequest clientRequest, HttpServletResponse proxyResponse, Response serverResponse) {
        if ("gzip".equals(serverResponse.getHeaders().get(HttpHeader.CONTENT_ENCODING))) {
            return new GZIPContentTransformer(newHttpClient(), new PageContentTransformer(clientRequest, proxyResponse, serverResponse));
        }
        return new PageContentTransformer(clientRequest, proxyResponse, serverResponse);
    }

}
