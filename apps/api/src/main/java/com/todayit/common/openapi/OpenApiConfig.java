package com.todayit.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Today-it OpenAPI 문서의 공통 정보를 구성합니다. */
@Configuration
public class OpenApiConfig {
  public static final String BEARER_AUTH = "bearerAuth";

  /**
   * API 문서 정보와 JWT 인증 방식을 구성합니다.
   *
   * @return Today-it OpenAPI 설정
   */
  @Bean
  public OpenAPI todayItOpenApi() {
    return new OpenAPI()
        .info(new Info().title("Today-it API").description("Today-it 백엔드 API 명세").version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
