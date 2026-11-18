package WebServer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.jsoup.Jsoup;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebProxyTest {

    private static final byte[] BINARY = {0, -1, 42};
    private static final String RAW_TARGET = "/Mixed%20Case?x=%2520&y=a+b;z=%2F";
    private static HttpServer upstream;
    private static ConfigurableApplicationContext application;
    private static HttpClient client;
    private static String server;
    private static String origin;
    private static String proxy;

    @BeforeAll
    static void startServers() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.createContext("/pages/index", exchange -> send(exchange, "text/html; charset=UTF-8",
                "<a href='../MixedCase?x=1&amp;y=a+b#part'>café</a>".getBytes(StandardCharsets.UTF_8)));
        upstream.createContext("/styles/main.css", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
                gzip.write("a{background:url(../Images/A.png)}".getBytes(StandardCharsets.UTF_8));
            }
            send(exchange, "text/css; charset=UTF-8", bytes.toByteArray());
        });
        upstream.createContext("/binary", exchange -> send(exchange, null, BINARY));
        upstream.createContext("/redirect", exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Location", "/pages/index");
                exchange.sendResponseHeaders(302, -1);
            }
        });
        upstream.createContext("/utf8", exchange -> send(exchange, "text/html",
                ("<a href='" + RAW_TARGET.replace("&", "&amp;") + "#part'>café 雪</a><a href='/雪'>snow</a>").getBytes(StandardCharsets.UTF_8)));
        // HttpServer matches context prefixes literally against the decoded path.
        upstream.createContext("/Mixed Case", exchange -> send(exchange, "text/plain; charset=UTF-8",
                exchange.getRequestURI().toString().getBytes(StandardCharsets.UTF_8)));
        upstream.createContext("/large-html", exchange -> sendRepresentation(exchange, "text/html", largeHtml(), false));
        upstream.createContext("/large-css", exchange -> sendRepresentation(exchange, "text/css", largeCss(), true));
        upstream.createContext("/gzip-binary", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            sendRepresentation(exchange, "application/octet-stream", gzip(BINARY), false);
        });
        upstream.createContext("/gzip-large-css", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            sendRepresentation(exchange, "text/css", gzip(largeCss()), false);
        });
        upstream.createContext("/stacked-encoding", exchange -> {
            exchange.getResponseHeaders().add("Content-Encoding", "gzip");
            exchange.getResponseHeaders().add("Content-Encoding", "br");
            send(exchange, "text/html", BINARY);
        });
        upstream.createContext("/digest-html", exchange -> sendRepresentation(exchange, "text/html",
                "<a href='/target'>link</a>".getBytes(StandardCharsets.UTF_8), false));
        upstream.start();

        application = SpringApplication.run(JettyServer.class, "--server.port=0");
        client = HttpClient.newHttpClient();
        int port = ((ServletWebServerApplicationContext) application).getWebServer().getPort();
        server = "http://127.0.0.1:" + port;
        origin = "http://127.0.0.1:" + upstream.getAddress().getPort();
        proxy = server + "/proxy?url=" + origin;
    }

    @AfterAll
    static void stopServers() {
        if (client != null) {
            client.close();
        }
        if (application != null) {
            application.close();
        }
        if (upstream != null) {
            upstream.stop(0);
        }
    }

    @Test
    void servesLocalEndpointsAndProxiesUpstreamResponses() throws Exception {
        assertTrue(get(client, server + "/").body().contains("It Works!"));
        assertTrue(get(client, server + "/hello").body().contains("Hello World!"));
        assertEquals(403, get(client, server + "/proxy?url=file:///etc/example").statusCode());

        var html = get(client, proxy + "/pages/index");
        assertEquals(200, html.statusCode());
        assertTrue(html.body().contains("/proxy?url=" + origin + "/MixedCase?x=1%26y=a%2Bb#part"));
        assertTrue(html.body().contains("café"));

        var css = client.send(HttpRequest.newBuilder(URI.create(proxy + "/styles/main.css")).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, css.statusCode());
        assertEquals("gzip", css.headers().firstValue("Content-Encoding").orElseThrow());
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(css.body()))) {
            assertTrue(new String(gzip.readAllBytes(), StandardCharsets.UTF_8)
                    .contains("/proxy?url=" + origin + "/Images/A.png"));
        }

        var untyped = client.send(HttpRequest.newBuilder(URI.create(proxy + "/binary")).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, untyped.statusCode());
        assertArrayEquals(BINARY, untyped.body());

        var redirect = get(client, proxy + "/redirect");
        assertEquals(302, redirect.statusCode());
        assertEquals("/proxy?url=" + origin + "/pages/index",
                redirect.headers().firstValue("Location").orElseThrow());
    }

    @Test
    void rewritesUtf8WithoutAnHttpCharsetAndPreservesRawTarget() throws Exception {
        var response = get(client, proxy + "/utf8");
        var document = Jsoup.parse(response.body());
        var anchor = document.selectFirst("a");
        assertEquals("café 雪", anchor.text());
        assertEquals("text/html;charset=utf-8", response.headers().firstValue("Content-Type")
                .orElseThrow().toLowerCase(Locale.ROOT).replace(" ", ""));
        assertTrue(anchor.attr("href").endsWith("#part"));
        assertTrue(document.select("a").get(1).attr("href").endsWith("/%25E9%259B%25AA"));
        assertEquals(RAW_TARGET, get(client, server + anchor.attr("href").split("#", 2)[0]).body());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/large-html", "/large-css", "/gzip-large-css"})
    void forwardsOversizedRepresentations(String path) throws Exception {
        var response = getBytes(path);
        assertEquals(200, response.statusCode());
        assertEquals("public, max-age=60", response.headers().firstValue("Cache-Control").orElseThrow());
        byte[] body = response.body();
        if (path.startsWith("/gzip")) {
            assertEquals("gzip", response.headers().firstValue("Content-Encoding").orElseThrow());
            try (var gzip = new GZIPInputStream(new ByteArrayInputStream(body))) {
                body = gzip.readAllBytes();
            }
        } else {
            assertEquals("\"upstream\"", response.headers().firstValue("ETag").orElseThrow());
            assertTrue(response.headers().firstValue("Content-Digest").isPresent());
        }
        assertArrayEquals(path.endsWith("html") ? largeHtml() : largeCss(), body);
    }

    @Test
    void keepsBinaryHeadersAndDropsStaleDigestFromRewrittenHtml() throws Exception {
        var binary = getBytes("/gzip-binary");
        assertEquals(200, binary.statusCode());
        assertEquals("gzip", binary.headers().firstValue("Content-Encoding").orElseThrow());
        assertEquals("application/octet-stream", binary.headers().firstValue("Content-Type").orElseThrow());
        assertEquals("\"upstream\"", binary.headers().firstValue("ETag").orElseThrow());
        assertTrue(binary.headers().firstValue("Content-Digest").isPresent());
        assertArrayEquals(gzip(BINARY), binary.body());

        var html = getBytes("/digest-html");
        assertEquals(200, html.statusCode());
        assertTrue(html.headers().firstValue("ETag").isEmpty());
        assertTrue(html.headers().firstValue("Content-Digest").isEmpty());
    }

    @Test
    void forwardsUnsupportedCodingStacksWithoutDecoding() throws Exception {
        var response = getBytes("/stacked-encoding");
        assertEquals(200, response.statusCode());
        assertEquals("gzip,br", String.join(",", response.headers().allValues("Content-Encoding")).replace(" ", ""));
        assertArrayEquals(BINARY, response.body());
    }

    private static HttpResponse<byte[]> getBytes(String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(proxy + path)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
    }

    private static byte[] largeHtml() {
        return ("<a href='/original'>original</a>" + "x".repeat(4 * 1024 * 1024)).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] largeCss() {
        return ("a{background:url('/original')}" + " ".repeat(4 * 1024 * 1024)).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] gzip(byte[] content) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(content);
        }
        return bytes.toByteArray();
    }

    private static void sendRepresentation(HttpExchange exchange, String contentType, byte[] bytes, boolean chunked)
            throws IOException {
        exchange.getResponseHeaders().set("ETag", "\"upstream\"");
        exchange.getResponseHeaders().set("Cache-Control", "public, max-age=60");
        exchange.getResponseHeaders().set("Content-Digest", "sha-256=:"
                + java.util.Base64.getEncoder().encodeToString(digest(bytes)) + ":");
        try (exchange) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, chunked ? 0 : bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private static byte[] digest(byte[] bytes) throws IOException {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IOException(exception);
        }
    }

    private static HttpResponse<String> get(HttpClient client, String url) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void send(HttpExchange exchange, String contentType, byte[] bytes) throws IOException {
        try (exchange) {
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-Type", contentType);
            }
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }
}
