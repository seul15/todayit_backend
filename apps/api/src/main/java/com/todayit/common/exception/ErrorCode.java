package com.todayit.common.exception;

/** 업무 예외에서 사용하는 오류 코드 계약입니다. */
public interface ErrorCode {

  /**
   * 클라이언트에 전달할 오류 코드를 반환합니다.
   *
   * @return 오류 코드
   */
  String code();

  /**
   * 클라이언트에 전달할 기본 오류 메시지를 반환합니다.
   *
   * @return 오류 메시지
   */
  String message();

  /**
   * 오류의 응답 상태 구분을 반환합니다.
   *
   * @return 오류 상태
   */
  ErrorStatus status();
}
