package com.todayit.common.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 인증되지 않은 사용자가 인증이 필요한 API에 접근시 HTTP 401 응답 반환하는 클래스 Controller에 도달 전에 Spring Security에서 인증 여부 확인
 * -> 인증 정보가 없거나 인증 실패 시 -> AuthenticationException 발생 -> 이 클래스 호출 인증 실패를 공통 JSON 오류 응답 형식으로 변환해줌
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  // 임시 오류 코드 입니다.
  private static final String ERROR_CODE = "AUTHENTICATION_REQUIRED";
  private static final String ERROR_MESSAGE = "인증이 필요합니다.";

  // 클라이언트에게 전달되는 인증 실패 메세지
  private final JsonMapper jsonMapper;

  /**
   * JsonMapper 주입
   *
   * @param jsonMapper JSON 변환 객체
   */
  public RestAuthenticationEntryPoint(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authenticationException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 인증되지 않은 요청 -> HTTP 상태 코드를 401로
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    Map<String, Object> errorResponse =
        Map.of(
            "success", false,
            "code", ERROR_CODE,
            "message", ERROR_MESSAGE);

    jsonMapper.writeValue(response.getOutputStream(), errorResponse);
  }
}
