package com.todayit.common.file.service.model;

import java.util.List;

/**
 * 파일 등록 결과와 외부 저장소에서 정리할 이전 프로필 정보입니다.
 *
 * @param files 저장한 파일 목록
 * @param replaced 교체된 이전 프로필 파일 목록
 */
public record FileRegistrationResult(List<FileResult> files, List<FileResult> replaced) {
  /**
   * 등록 결과 목록의 변경을 막기 위해 복사합니다.
   *
   * @param files 저장한 파일 목록
   * @param replaced 교체된 이전 프로필 파일 목록
   */
  public FileRegistrationResult {
    files = List.copyOf(files);
    replaced = List.copyOf(replaced);
  }
}
