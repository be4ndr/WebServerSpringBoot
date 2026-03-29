# WebServerSpringBoot

A Spring Boot + Jetty-based web proxy server that rewrites HTML/CSS resource URLs so browser requests can be routed through the proxy endpoint.

## Features

- Embedded Spring Boot web server using Jetty.
- Proxy servlet based on Jetty middle-man proxy support.
- HTML transformation for common URL-bearing attributes (`href`, `src`, etc.).
- CSS URL rewriting using `ph-css` visitor APIs.
- Unit tests for URL rewrite behavior and stream replacement utilities.

## Requirements

- Java 17+
- Maven 3.9+

## Build & Test

```bash
mvn test
```

## Run

```bash
mvn spring-boot:run
```

By default, the application starts using the Spring Boot configuration in this repository.

## Project Structure

- `src/main/java/WebServer` – application and proxy/transformation logic.
- `src/test/java/WebServer` – unit tests.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
