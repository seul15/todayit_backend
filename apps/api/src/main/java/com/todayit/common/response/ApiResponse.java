package com.todayit.common.response;

/**
 * 성공 API 응답의 공통 형식입니다.
 *
 * @param <T> 응답 데이터 타입
 * @param success 요청 성공 여부
 * @param data 응답 데이터
 */
public record ApiResponse<T>(boolean success, T data) {

  /**
   * 성공 응답을 생성합니다.
   *
   * @param data 응답 데이터
   * @param <T> 응답 데이터 타입
   * @return 성공 API 응답
   */
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data);
  }
}
