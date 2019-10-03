
package WebServer;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;

public class ReplaceInputStreamTest {

    private byte[] bytes;
    private ByteArrayInputStream bis;
    private ReplaceInputStream ris;
    private ByteArrayOutputStream bos;

    @BeforeMethod
    public void beforeTest() throws Exception {
        bytes = "hello zxy world.".getBytes("UTF-8");
        bis = new ByteArrayInputStream(bytes);
    }

    @Test
    public void testReplacingInputStream() throws Exception {
        ris = new ReplaceInputStream(bis, "zxy", "abc");
        bos = new ByteArrayOutputStream();
        int b;
        while (-1 != (b = ris.read()))
            bos.write(b);
        assertEquals("hello abc world.", bos.toString());
    }


    @Test
    public void testReplacingToEmptyString() throws Exception {
        ris = new ReplaceInputStream(bis, "xyz", "");
        bos = new ByteArrayOutputStream();

        int b;
        while (-1 != (b = ris.read()))
            bos.write(b);

        assertEquals("hello  world.", bos.toString());
    }
}

