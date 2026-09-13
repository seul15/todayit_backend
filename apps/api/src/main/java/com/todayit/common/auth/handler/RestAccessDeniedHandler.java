package com.todayit.common.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 인증된 사용자가 접근 권한이 없는 API를 요청시 HTTP 403 응답 반환하는 클래스 Spring Security가 권한이 없다 판단 ->
 * AccessDeniedException 발생 -> 이 클래스 호출 인가 실패를 공통 JSON 오류 응답 형식으로 변환해줌
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  // 임시 오류 코드입니다.
  private static final String ERROR_CODE = "ACCESS_DENIED";
  private static final String ERROR_MESSAGE = "접근 권한이 없습니다.";

  // 클라이언트에게 전달되는 인증 실패 메세지
  private final JsonMapper jsonMapper;

  /**
   * JsonMapper 주입
   *
   * @param jsonMapper JSON 변환 객체
   */
  public RestAccessDeniedHandler(JsonMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 권한이 없는 요청 -> HTTP 상태 코드를 403으로
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
