package com.blog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 接口文档顶部信息
                .info(new Info()
                        .title("Diamond个人博客平台 API 接口文档")
                        .description("Diamond个人博客平台后端 RESTful API，基于 Spring Boot 3 + Sa-Token + MyBatis 构建")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Diamond")
                                .email("diamond@blog.com")
                                .url("http://d1amond.cn"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                // 全局 Authorization 请求头（Sa-Token Bearer）
                .addSecurityItem(new SecurityRequirement().addList("Authorization"))
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Authorization")
                                        .description("Sa-Token 令牌，登录后获取")));
    }
}
