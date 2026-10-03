package com.todayit.common.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.todayit.common.exception.ErrorCode;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 공통 오류 응답입니다.
 *
 * @param success 요청 성공 여부
 * @param code 오류 코드
 * @param message 외부 공개 오류 메시지
 * @param details 응답의 최상위에 추가할 오류 상세 정보
 */
public record ErrorResponse(
    boolean success, String code, String message, @JsonIgnore Map<String, Object> details) {

  /** 상세 정보를 변경할 수 없는 Map으로 복사합니다. */
  public ErrorResponse {
    details =
        details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
  }

  /**
   * 상세 정보를 응답 최상위에 펼쳐서 반환합니다.
   *
   * @return 오류 상세 정보
   */
  @JsonAnyGetter
  public Map<String, Object> additionalFields() {
    return details;
  }

  /**
   * 오류 코드와 메시지로 실패 응답을 생성합니다.
   *
   * @param errorCode 오류 코드
   * @param message 외부 공개 오류 메시지
   * @param details 응답의 최상위에 추가할 오류 상세 정보
   * @return 공통 오류 응답
   */
  public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
    return new ErrorResponse(false, errorCode.code(), message, details);
  }
}
