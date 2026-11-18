# WebServerSpringBoot

A Spring Boot + Jetty-based web proxy server that rewrites HTML/CSS resource URLs so browser requests can be routed through the proxy endpoint.

## Features

- Embedded Spring Boot web server using Jetty.
- Proxy servlet based on Jetty middle-man proxy support.
- HTML transformation for common URL-bearing attributes (`href`, `src`, etc.).
- CSS URL rewriting using `ph-css` visitor APIs.
- Unit tests for URL rewrite behavior and stream replacement utilities.

## Requirements

- Java 25 LTS
- Maven 3.10.1

## Build & Test

```bash
mvn clean verify
```

## Run

```bash
mvn spring-boot:run
```

The application listens on `127.0.0.1:8090` by default. Use `server.port` to change the port.
The proxy accepts arbitrary HTTP(S) destinations and serves upstream content on its own origin.
Keep it on loopback for trusted local use.

## Project Structure

- `src/main/java/WebServer` – application and proxy/transformation logic.
- `src/test/java/WebServer` – unit tests.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
