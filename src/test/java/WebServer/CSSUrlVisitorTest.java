package WebServer;

import com.helger.css.ECSSVersion;
import com.helger.css.decl.CascadingStyleSheet;
import com.helger.css.decl.visit.CSSVisitor;
import com.helger.css.reader.CSSReader;
import com.helger.css.writer.CSSWriter;
import com.helger.css.writer.CSSWriterSettings;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CSSUrlVisitorTest {
    @DataProvider
    public Object[][] cssDataProvider() {
        return new Object[][]{
                {"div{background:url(../images/icons.svg)}","div{background:url(http://test.com?url=http://querytest.com/images/icons.svg)}"},
                {"div{list-style-image:url(../images/list-item.svg)}","div{list-style-image:url(http://test.com?url=http://querytest.com/images/list-item.svg)}"},
                {"div{background-image:url(/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)}","div{background-image:url(http://test.com?url=http://querytest.com/img2019/sprite-projectsico.110ea83ea4acdeb19c46.png)}"},
//                {"div{background:url(data:image/gif;base64,R0lGODlhCgAKALMAAGZmZu/v77W1tYyMjNPT06Wlpf///8zMzGZmZuPj462trff399/f33JycgAAAAAAACH5BAQUAP8ALAAAAAAKAAoAAAQxMAQyABhEBtWEVIAyNYlhGklDCeepVEtrJlYgG8zFygUGMC0CIMMpMBiFhkhCsWAkEQA7)}","div{background:url(data:image/gif;base64,R0lGODlhCgAKALMAAGZmZu/v77W1tYyMjNPT06Wlpf///8zMzGZmZuPj462trff399/f33JycgAAAAAAACH5BAQUAP8ALAAAAAAKAAoAAAQxMAQyABhEBtWEVIAyNYlhGklDCeepVEtrJlYgG8zFygUGMC0CIMMpMBiFhkhCsWAkEQA7)}"}
        };
    }

    @Test(dataProvider = "cssDataProvider")
    public void shouldReplaceCssUrl(String originCss, String expectedCss) {
        CSSWriterSettings aSettings = new CSSWriterSettings();
        aSettings.setOptimizedOutput(true);
        CSSWriter cssWriter = new CSSWriter(aSettings);
        String baseUri = "http://test.com?url=";
        String originUri = "http://querytest.com/";
        CascadingStyleSheet css = CSSReader.readFromString(originCss, ECSSVersion.LATEST);
        CSSVisitor.visitCSSUrl(css,new CSSUrlVisitor(baseUri,originUri));
        String cssString = cssWriter.getCSSAsString(css);
        Assert.assertEquals(cssString,expectedCss);
    }
}