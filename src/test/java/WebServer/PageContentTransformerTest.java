package WebServer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.ByteBuffer;


import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

public class PageContentTransformerTest {
    HttpServletRequest request;
    HttpServletResponse response;
    PageContentTransformer transformer;
    Document doc;

    @BeforeMethod
    public void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        doc = Mockito.mock(Document.class);
        transformer = new PageContentTransformer(request, response, null);
    }

    @Test
    public void shouldExtractURL() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com");
        String s = transformer.extractURI(request);
        Assert.assertEquals(s, "http://test.com?url=http://querytest.com");
    }

    @Test
    public void shouldConvertByteBufferToString() throws IOException {
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
        String s = "OriginalString";
        ByteBuffer bb = ByteBuffer.wrap(s.getBytes());
        String converted = transformer.byteBufferToString(bb);
        Assert.assertEquals(s, converted);
    }

    @Test
    public void shouldConvertStringToByteBuffer() {
        when(response.getCharacterEncoding()).thenReturn("UTF-8");
        String s = "OriginalString";
        ByteBuffer a = ByteBuffer.wrap(s.getBytes());
        ByteBuffer b = transformer.stringToByteBuffer(s);
        Assert.assertEquals(a, b);
    }

    @DataProvider
    public Object[][] urlDataProvider() {
        return new Object[][]{
                {"http://querytest.com/css/main.css", "http://test.com?url=http://querytest.com/css/main.css"},
                {"/css/main.css", "http://test.com?url=http://querytest.com/css/main.css"},
                {"//css/main.com", "http://test.com?url=http://css/main.com"},
//                {"./css/main.css","http://test.com?url=http://querytest.com/css/main.css"},
                {"../css/main.css", "http://test.com?url=http://querytest.com/css/main.css"},
                {"css/main.css", "http://test.com?url=http://querytest.com/css/main.css"},
                {"../../css/main.css", "http://test.com?url=http://querytest.com/css/main.css"},//Advanced level
                {"/css/../main.css", "http://test.com?url=http://querytest.com/main.css"},//Advanced level
                {"/css/../../main.css", "http://test.com?url=http://querytest.com/main.css"},//Advanced level
                {"/css/style/main/../../main.css", "http://test.com?url=http://querytest.com/css/main.css"},//Advanced level
        };
    }

    @Test(dataProvider = "urlDataProvider")
    public void shouldReplaceDomElements(String originUrl, String expectedUrl) throws IOException {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com");
        Elements l = Mockito.mock(Elements.class);
        Element e = Mockito.mock(Element.class);
        when(e.attr(eq("href"))).thenReturn(originUrl);
        when(l.toArray(any())).thenReturn(new Element[]{e});
        when(doc.select(eq("a[href]"))).thenReturn(l);
        transformer.modifyElements(doc, "a[href]", "href");
        Mockito.verify(e).attr(eq("href"), eq(expectedUrl));
    }

    @Test(dataProvider = "urlDataProvider")
    public void shouldReplaceUri(String resourceUri, String expectedUri) throws IOException {
/*        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com/");*/
        String baseUri = "http://test.com?url=";
        String originUri = "url=http://querytest.com/";
        originUri = originUri.replace("url=", "");
        String result = UrlUtils.modifyURI(baseUri, originUri, resourceUri);
        Assert.assertEquals(result, expectedUri);
    }

    @DataProvider
    public Object[][] cssDataProvider() {
        return new Object[][]{
                {"background:url(../images/icons.svg)", "background:url(http://test.com?url=http://querytest.com/images/icons.svg)"},
                {"list-style-image:url(../images/list-item.svg)", "list-style-image:url(http://test.com?url=http://querytest.com/images/list-item.svg)"},
                {"background-image:url(/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)", "background-image:url(http://test.com?url=http://querytest.com/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)}"},
                {"background:url(data:image/gif;base64,R0lGODlhCgAKALMAAGZmZu/v77W1tYyMjNPT06Wlpf///8zMzGZmZuPj462trff399/f33JycgAAAAAAACH5BAQUAP8ALAAAAAAKAAoAAAQxMAQyABhEBtWEVIAyNYlhGklDCeepVEtrJlYgG8zFygUGMC0CIMMpMBiFhkhCsWAkEQA7)", "background:url(data:image/gif;base64,R0lGODlhCgAKALMAAGZmZu/v77W1tYyMjNPT06Wlpf///8zMzGZmZuPj462trff399/f33JycgAAAAAAACH5BAQUAP8ALAAAAAAKAAoAAAQxMAQyABhEBtWEVIAyNYlhGklDCeepVEtrJlYgG8zFygUGMC0CIMMpMBiFhkhCsWAkEQA7)"}
        };
    }

    @Test(dataProvider = "cssDataProvider")
    public void shouldReplaceCssUrl(String originUrl, String expectedUrl) {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com/");
        String baseUri = request.getRequestURL() + "?url=";
        String originUri = request.getQueryString();

    }
}