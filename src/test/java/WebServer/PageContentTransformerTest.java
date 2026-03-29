package WebServer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PageContentTransformerTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private PageContentTransformer transformer;
    private Document document;

    @BeforeEach
    void setUp() {
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
        document = Mockito.mock(Document.class);
        transformer = new PageContentTransformer(request, response, null);
    }

    @Test
    void shouldExtractUrl() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com");

        String actual = transformer.extractURI(request);

        assertEquals("http://test.com?url=http://querytest.com", actual);
    }

    @Test
    void shouldConvertByteBufferToString() throws IOException {
        when(response.getCharacterEncoding()).thenReturn("UTF-8");

        String input = "OriginalString";
        String actual = transformer.byteBufferToString(ByteBuffer.wrap(input.getBytes()));

        assertEquals(input, actual);
    }

    @Test
    void shouldConvertStringToByteBuffer() {
        when(response.getCharacterEncoding()).thenReturn("UTF-8");

        String input = "OriginalString";
        ByteBuffer expected = ByteBuffer.wrap(input.getBytes());

        assertEquals(expected, transformer.stringToByteBuffer(input));
    }

    @ParameterizedTest
    @MethodSource("urlCases")
    void shouldReplaceDomElements(String originalUrl, String expectedUrl) {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://test.com"));
        when(request.getQueryString()).thenReturn("url=http://querytest.com");

        Elements elements = Mockito.mock(Elements.class);
        Element element = Mockito.mock(Element.class);
        when(element.attr(eq("href"))).thenReturn(originalUrl);
        when(elements.toArray(any(Element[].class))).thenReturn(new Element[]{element});
        when(document.select(eq("a[href]"))).thenReturn(elements);

        transformer.modifyElements(document, "a[href]", "href");

        verify(element).attr(eq("href"), eq(expectedUrl));
    }

    @ParameterizedTest
    @MethodSource("urlCases")
    void shouldReplaceUri(String resourceUri, String expectedUri) {
        String result = UrlUtils.modifyURI("http://test.com?url=", "http://querytest.com/", resourceUri);
        assertEquals(expectedUri, result);
    }

    private static Stream<Arguments> urlCases() {
        return Stream.of(
                Arguments.of("http://querytest.com/css/main.css", "http://test.com?url=http://querytest.com/css/main.css"),
                Arguments.of("/css/main.css", "http://test.com?url=http://querytest.com/css/main.css"),
                Arguments.of("//css/main.com", "http://test.com?url=http://css/main.com"),
                Arguments.of("../css/main.css", "http://test.com?url=http://querytest.com/css/main.css"),
                Arguments.of("css/main.css", "http://test.com?url=http://querytest.com/css/main.css"),
                Arguments.of("../../css/main.css", "http://test.com?url=http://querytest.com/css/main.css"),
                Arguments.of("/css/../main.css", "http://test.com?url=http://querytest.com/main.css"),
                Arguments.of("/css/../../main.css", "http://test.com?url=http://querytest.com/main.css"),
                Arguments.of("/css/style/main/../../main.css", "http://test.com?url=http://querytest.com/css/main.css")
        );
    }
}
