package com.todayit.common.pagination;

import java.util.List;

/**
 * 서비스 계층에서 사용하는 페이지 조회 결과입니다.
 *
 * @param <T> 목록 요소 타입
 * @param content 목록
 * @param page 현재 페이지 번호
 * @param size 페이지 크기
 * @param totalElements 전체 요소 수
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

  /** 페이지 조회 결과를 생성합니다. */
  public PageResult {
    content = List.copyOf(content);
  }
}
