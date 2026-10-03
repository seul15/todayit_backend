package com.todayit.place.exception;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.ErrorCode;

/** 장소 업무 규칙을 위반했을 때 발생하는 예외의 공통 골격입니다. */
public abstract class PlaceException extends BusinessException {

  /**
   * 장소 오류 코드로 업무 예외를 생성합니다.
   *
   * @param errorCode 장소 기능에서 발생한 오류 코드
   */
  protected PlaceException(ErrorCode errorCode) {
    super(errorCode);
  }
}
