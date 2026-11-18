package WebServer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlUtilsTest {

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
