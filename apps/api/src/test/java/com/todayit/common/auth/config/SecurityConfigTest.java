package com.todayit.common.auth.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.auth.handler.RestAccessDeniedHandler;
import com.todayit.common.auth.handler.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// Spring Security 접근 정책, 인증 인가 실패 응답 테스트
@SpringBootTest(properties = "todayit.security.cors.origins[0]=https://frontend.test")
@AutoConfigureMockMvc
@Import({
  SecurityConfigTest.SecurityTestController.class,
  SecurityConfigTest.AdminSecurityTestConfig.class
})
public class SecurityConfigTest {

  private final MockMvc mockMvc;

  @Autowired
  SecurityConfigTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void allowsAnonymousAccessToPublicEndpoint() throws Exception {
    mockMvc.perform(post("/api/v1/auth/login")).andExpect(status().isOk());
  }

  @Test
  void returnsUnauthorizedWhenAnonymousUserAccessesProtectedEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/v1/security/protected"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false)) // $.success -> JSON 안의 값을 찾는 경로
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.message").value("인증이 필요합니다."));
  }

  @Test
  @WithMockUser(roles = "USER")
  void returnsForbiddenWhenAuthenticatedUserLacksRequiredRole() throws Exception {
    mockMvc
        .perform(get("/api/v1/security/admin/test"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
        .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
  }

  // 허용된 Origin의 CORS preflight → 200 → Access-Control-Allow-Origin 확인
  @Test
  void allowsCorsPreflightRequestFromConfiguredOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://frontend.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.test"));
  }

  // CorsProperties의 허용 목록 확인 -> 일치하지 않음 -> CORS 단계에서 차단 -> 403
  @Test
  void rejectsCorsPreflightRequestFromUnconfiguredOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/login")
                .header(HttpHeaders.ORIGIN, "https://not-allowed.test")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  // 테스트를 위해서 사용하는 임시 Controller
  @RestController
  static class SecurityTestController {

    @PostMapping("/api/v1/auth/login")
    ResponseEntity<Void> login() {
      return ResponseEntity.ok().build();
    }

    // 401 응답 테스트
    @GetMapping("/api/v1/security/protected")
    ResponseEntity<Void> protectedEndpoint() {
      return ResponseEntity.ok().build();
    }

    // 403 응답 테스트
    @GetMapping("/api/v1/security/admin/test")
    ResponseEntity<Void> adminEndpoint() {
      return ResponseEntity.ok().build();
    }
  }

  // 403 응답 테스트를 위한 권한 제한 설정
  @TestConfiguration(proxyBeanMethods = false)
  static class AdminSecurityTestConfig {

    @Bean
    @Order(1) // 기존보다 먼저 검사
    SecurityFilterChain adminTestSecurityFilterChain(
        HttpSecurity http,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler)
        throws Exception {

      return http
          // SecurityFilterChain은 아래 경로에만 적용됩니다.
          .securityMatcher("/api/v1/security/admin/**")
          .csrf(csrf -> csrf.disable())

          // ADMIN 역할을 가진 사용자만 접근 가능
          .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("ADMIN"))

          // 실제로 작성한 401/403 Handler가 호출되는지 함께 검증합니다.
          .exceptionHandling(
              exception ->
                  exception
                      .authenticationEntryPoint(authenticationEntryPoint)
                      .accessDeniedHandler(accessDeniedHandler))
          .build();
    }
  }
}
