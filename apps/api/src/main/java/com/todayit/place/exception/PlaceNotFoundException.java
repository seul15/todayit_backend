package com.todayit.place.exception;

/** 장소를 찾을 수 없을 때 발생하는 예외입니다. */
public class PlaceNotFoundException extends PlaceException {

  /** 장소 없음 오류를 생성합니다. */
  public PlaceNotFoundException() {
    super(PlaceErrorCode.NOT_FOUND);
  }
}
