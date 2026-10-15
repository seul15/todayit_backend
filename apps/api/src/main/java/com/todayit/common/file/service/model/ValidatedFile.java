package com.todayit.common.file.service.model;

import com.todayit.common.file.validation.FileExtension;

/**
 * 검증이 끝난 업로드 파일입니다.
 *
 * @param originalName 원본 파일명
 * @param extension 검증한 확장자
 * @param content 검증한 바이트
 */
public record ValidatedFile(String originalName, FileExtension extension, byte[] content) {
  /**
   * 파일 내용을 방어적으로 복사합니다.
   *
   * @param originalName 원본 파일명
   * @param extension 검증한 확장자
   * @param content 파일 내용
   */
  public ValidatedFile {
    content = content.clone();
  }

  /**
   * 검증된 파일 내용의 복사본을 반환하여 내부 바이트 배열이 변경되지 않게 합니다.
   *
   * @return 파일 내용의 복사본
   */
  @Override
  public byte[] content() {
    return content.clone();
  }

  /**
   * 내용을 복사하지 않고 파일 크기를 반환합니다.
   *
   * @return 바이트 크기
   */
  public int sizeBytes() {
    return content.length;
  }
}
