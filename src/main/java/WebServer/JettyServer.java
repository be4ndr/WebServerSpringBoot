package WebServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JettyServer {
    public static void main(String[] args) {
        SpringApplication.run(JettyServer.class, args);
    }
}
