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
 * @param totalPages 전체 페이지 수
 */
public record PageResult<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /** 페이지 조회 결과를 생성합니다. */
  public PageResult {
    content = List.copyOf(content);
  }

  /**
   * 전체 요소 수로 전체 페이지 수를 계산해 페이지 결과를 생성합니다.
   *
   * @param content 목록
   * @param page 현재 페이지 번호
   * @param size 페이지 크기
   * @param totalElements 전체 요소 수
   */
  public PageResult(List<T> content, int page, int size, long totalElements) {
    this(content, page, size, totalElements, calculateTotalPages(size, totalElements));
  }

  private static int calculateTotalPages(int size, long totalElements) {
    return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
  }
}
