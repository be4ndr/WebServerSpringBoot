package WebServer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplaceInputStreamTest {

    @ParameterizedTest
    @CsvSource({
            "hello zxy world.|zxy|abc|hello abc world.",
            "hello xyz world.|xyz||hello  world."
    })
    void shouldReplaceInputStreamContent(String input, String search, String replacement, String expected) throws IOException {
        ByteArrayInputStream source = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        ReplaceInputStream stream = new ReplaceInputStream(source, search, replacement);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int value;
        while ((value = stream.read()) != -1) {
            output.write(value);
        }

        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }
}
