package com.todayit.common.pagination;

import com.todayit.common.exception.InvalidPaginationException;

/** 여러 API에서 사용하는 페이지 요청값의 공통 조건을 검증합니다. */
public final class PaginationValidator {

  /** 인스턴스 생성을 방지합니다. */
  private PaginationValidator() {}

  /**
   * 페이지 요청값이 유효한지 확인합니다.
   *
   * @param page 0 이상이어야 하는 페이지 번호
   * @param size 1 이상이어야 하는 페이지 크기
   * @throws InvalidPaginationException page가 0보다 작거나 size가 1보다 작을 때
   */
  public static void validate(int page, int size) {
    if (page < 0 || size <= 0) {
      throw new InvalidPaginationException();
    }
  }
}
