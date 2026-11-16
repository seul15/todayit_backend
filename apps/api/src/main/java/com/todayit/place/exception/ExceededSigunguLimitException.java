package com.todayit.place.exception;

/** 시도당 시군구 필터 개수 제한(3개)을 초과했을 때 발생하는 예외입니다. */
public class ExceededSigunguLimitException extends PlaceException {

  /** 시군구 필터 개수 초과 오류를 생성합니다. */
  public ExceededSigunguLimitException() {
    super(PlaceErrorCode.EXCEEDED_SIGUNGU_LIMIT);
  }
}
