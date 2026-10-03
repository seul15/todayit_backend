package com.todayit.common.exception;

/** 페이지 요청값이 올바르지 않을 때 발생하는 예외입니다. */
public class InvalidPaginationException extends BusinessException {

  /** 페이지 번호 또는 크기가 올바르지 않은 경우의 예외를 생성합니다. */
  public InvalidPaginationException() {
    super(CommonErrorCode.INVALID_PAGINATION);
  }
}
