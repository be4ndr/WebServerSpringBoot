package WebServer;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSSUrlVisitorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "div{background:url(../images/icons.svg)}|div{background:url(http://test.com?url=http://querytest.com/images/icons.svg)}",
            "div{list-style-image:url(../images/list-item.svg)}|div{list-style-image:url(http://test.com?url=http://querytest.com/images/list-item.svg)}",
            "div{background-image:url(/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)}|div{background-image:url(http://test.com?url=http://querytest.com/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)}"
    })
    void shouldReplaceCssUrl(String originCss, String expectedCss) {
        CSSWriterSettings settings = new CSSWriterSettings();
        settings.setOptimizedOutput(true);

        CascadingStyleSheet css = CSSReader.readFromString(originCss, ECSSVersion.LATEST);
        CSSVisitor.visitCSSUrl(css, new CSSUrlVisitor("http://test.com?url=", "http://querytest.com/"));

        String actualCss = new CSSWriter(settings).getCSSAsString(css);
        assertEquals(expectedCss, actualCss);
    }
}
