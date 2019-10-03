package WebServer;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
@ComponentScan(value = "WebServer")
@EnableAutoConfiguration
public class AppConfig {

    @Bean
    ServletRegistrationBean<HelloServlet> helloServlet() {
        ServletRegistrationBean<HelloServlet> srb = new ServletRegistrationBean<>();
        srb.setServlet(new HelloServlet());
        srb.setUrlMappings(Collections.singletonList("/hello"));
        return srb;
    }

    @Bean
    ServletRegistrationBean<ItWorksServlet> itWorksServlet() {
        ServletRegistrationBean<ItWorksServlet> srb = new ServletRegistrationBean<>();
        srb.setServlet(new ItWorksServlet());
        srb.setUrlMappings(Collections.singletonList(""));
        return srb;
    }

    @Bean
    ServletRegistrationBean<WebProxyServlet> webProxyServlet() {
        ServletRegistrationBean<WebProxyServlet> srb = new ServletRegistrationBean<>();
        srb.setServlet(new WebProxyServlet());
        srb.setUrlMappings(Collections.singletonList("/proxy"));
        return srb;
    }

}
