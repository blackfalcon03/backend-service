package net.ey.backendservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .version("1.0.0")
                        .description("Order Management Microservice API with OpenAPI documentation for SDET Automation and AI AutoTriage demo.")
                        .contact(new Contact()
                                .name("SDET Platform Engineering")
                                .email("sdet-platform@example.com"))
                        .license(new License().name("Apache 2.0")));
    }
}
