package com.umc.devine.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "JWT";

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(servers())
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(securityComponents());
    }

    // TODO 설명 추가
    private Info apiInfo() {
        return new Info()
                .title("DeVine API")
                .version("v1.0.0")
                .description("")
                .contact(new Contact()
                        .name("DeVine Github")
                        .url("https://github.com/DeVine-2025"));
    }

    private List<Server> servers() {
        Server localServer = new Server()
                .url("/")
                .description("Current Server");

        // TODO 추후 개발 서버 분리
        // Server devServer = new Server()
        //         .url("https://dev.devine.kr")
        //         .description("Development Server");

        Server prodServer = new Server()
                .url("https://api.devine.kr")
                .description("Production Server");

        return "prod".equals(activeProfile)
                ? List.of(prodServer)
                : List.of(localServer, prodServer);
    }

    private Components securityComponents() {
        return new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 액세스 토큰을 입력하세요. (Bearer 접두사 불필요)"));
    }
}
