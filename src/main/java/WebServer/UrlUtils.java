package WebServer;

import java.net.URI;
import java.util.Locale;

public class UrlUtils {
    static URI httpUri(String value) {
        if (value == null) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && uri.getRawUserInfo() == null && uri.getPort() <= 65535) {
                return uri;
            }
        } catch (IllegalArgumentException ignored) {
            // The servlet rejects malformed targets.
        }
        return null;
    }

    public static String modifyURI(String baseUri, String originUri, String resourceUri) {
        URI origin = httpUri(originUri);
        String resource = resourceUri.strip();
        if (origin == null || resource.isEmpty() || resource.startsWith("#")) {
            return resourceUri;
        }

        try {
            URI reference = URI.create(resource.replace(" ", "%20"));
            if (reference.isOpaque()) {
                return resourceUri;
            }
            URI resolved;
            if (reference.getRawPath().isEmpty() && reference.getRawAuthority() == null
                    && reference.getRawQuery() != null) {
                // URI.resolve treats a query-only reference as relative to the parent directory.
                resolved = URI.create(origin.toASCIIString().split("[?#]", 2)[0] + reference);
            } else {
                resolved = origin.resolve(reference).normalize();
            }
            if (httpUri(resolved.toString()) == null) {
                return resourceUri;
            }
            String path = resolved.getRawPath();
            while (path.startsWith("/../") || path.equals("/..")) {
                path = path.length() == 3 ? "/" : path.substring(3);
            }
            String target = resolved.getScheme().toLowerCase(Locale.ROOT) + "://" + resolved.getRawAuthority()
                    + path + (resolved.getRawQuery() == null ? "" : "?" + resolved.getRawQuery());
            String fragment = resolved.getRawFragment();
            return baseUri + URI.create(target).toASCIIString().replace("%", "%25").replace("&", "%26")
                    .replace("+", "%2B").replace(";", "%3B") + (fragment == null ? "" : "#" + fragment);
        } catch (IllegalArgumentException ignored) {
            return resourceUri;
        }
    }
}
