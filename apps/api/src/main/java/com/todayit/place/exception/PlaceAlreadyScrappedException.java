package com.todayit.place.exception;

/** 이미 스크랩한 장소에 다시 스크랩을 요청했을 때 발생하는 예외입니다. */
public class PlaceAlreadyScrappedException extends PlaceException {

  /** 이미 스크랩한 장소 오류를 생성합니다. */
  public PlaceAlreadyScrappedException() {
    super(PlaceErrorCode.ALREADY_SCRAPPED);
  }
}
