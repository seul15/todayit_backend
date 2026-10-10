package com.todayit.place.exception;

/** 이미 좋아요를 누른 장소에 다시 좋아요를 요청했을 때 발생하는 예외입니다. */
public class PlaceAlreadyLikedException extends PlaceException {

  /** 이미 좋아요를 누른 장소 오류를 생성합니다. */
  public PlaceAlreadyLikedException() {
    super(PlaceErrorCode.ALREADY_LIKED);
  }
}
