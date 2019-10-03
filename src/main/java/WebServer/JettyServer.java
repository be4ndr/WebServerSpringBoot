package WebServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@ServletComponentScan("org.eclipse.jetty.util")
@SpringBootApplication
public class JettyServer extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder appBuilder) {
        return appBuilder.sources(AppConfig.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AppConfig.class, args);
    }
}
