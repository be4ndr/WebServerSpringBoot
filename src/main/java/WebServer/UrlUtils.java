package WebServer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    public static String modifyURI(String baseUri, String originUri, String resourceUri) {
        if (!originUri.endsWith("/")) {
            originUri = originUri + "/";
        }
        String url;
        if (!resourceUri.contains(originUri) && !resourceUri.startsWith("http://") && !resourceUri.startsWith("https://")) {
            url = (baseUri + "//").replaceAll("(.*)//$", "$1") + originUri + resourceUri.replaceAll("^/*", "");
        } else {
            url = (baseUri + "//").replaceAll("(.*)//$", "$1") + resourceUri.replaceAll("^/*", "");
        }
        Pattern p = Pattern.compile("/\\w*/\\.\\./");
        Matcher m = p.matcher(url);
        while (m.find()) {
            url = m.replaceAll("/");
            m = p.matcher(url);
            m.reset();
        }
        url = url.replace("../", "");
        return url;
    }
}