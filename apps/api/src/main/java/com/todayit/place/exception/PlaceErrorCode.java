package com.todayit.place.exception;

import com.todayit.common.exception.ErrorCode;
import com.todayit.common.exception.ErrorStatus;

/** 장소 기능에서 사용하는 오류 코드입니다. */
public enum PlaceErrorCode implements ErrorCode {
  /** 요청한 장소가 존재하지 않는 경우입니다. */
  NOT_FOUND("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),

  /** 이미 좋아요를 누른 장소에 다시 좋아요를 요청한 경우입니다. */
  ALREADY_LIKED("PLACE_ALREADY_LIKED", "이미 좋아요를 눌렀습니다.", ErrorStatus.BAD_REQUEST),

  /** 이미 스크랩한 장소에 다시 스크랩을 요청한 경우입니다. */
  ALREADY_SCRAPPED("PLACE_ALREADY_SCRAPPED", "이미 스크랩되었습니다.", ErrorStatus.BAD_REQUEST);

  /** 클라이언트에 전달할 오류 코드입니다. */
  private final String code;

  /** 클라이언트에 전달할 오류 메시지입니다. */
  private final String message;

  /** 오류를 변환할 HTTP 상태입니다. */
  private final ErrorStatus status;

  /**
   * 장소 오류 코드를 생성합니다.
   *
   * @param code 클라이언트에 전달할 오류 코드
   * @param message 클라이언트에 전달할 오류 메시지
   * @param status 오류를 변환할 HTTP 상태
   */
  PlaceErrorCode(String code, String message, ErrorStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  /**
   * 클라이언트에 전달할 오류 코드를 반환합니다.
   *
   * @return 오류 코드
   */
  @Override
  public String code() {
    return code;
  }

  /**
   * 클라이언트에 전달할 오류 메시지를 반환합니다.
   *
   * @return 오류 메시지
   */
  @Override
  public String message() {
    return message;
  }

  /**
   * 오류를 변환할 HTTP 상태를 반환합니다.
   *
   * @return 오류 상태
   */
  @Override
  public ErrorStatus status() {
    return status;
  }
}
