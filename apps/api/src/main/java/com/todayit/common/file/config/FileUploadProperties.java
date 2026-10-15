package com.todayit.common.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 문의 첨부파일의 개수와 누적 용량 제한입니다.
 *
 * @param inquiryMaxFiles 문의 하나에 첨부할 수 있는 최대 파일 개수
 * @param inquiryMaxTotalSize 기존 파일을 포함한 문의 첨부의 최대 합계 용량
 */
@ConfigurationProperties("todayit.file")
public record FileUploadProperties(int inquiryMaxFiles, DataSize inquiryMaxTotalSize) {
  /**
   * 문의 첨부 제한값이 양수인지 확인합니다.
   *
   * @param inquiryMaxFiles 최대 파일 개수
   * @param inquiryMaxTotalSize 최대 합계 용량
   * @throws IllegalArgumentException 개수나 용량이 설정되지 않았거나 양수가 아닐 때
   */
  public FileUploadProperties {
    if (inquiryMaxFiles < 1 || inquiryMaxTotalSize == null || inquiryMaxTotalSize.toBytes() < 1) {
      throw new IllegalArgumentException("문의 첨부의 최대 파일 개수와 합계 용량은 양수여야 합니다.");
    }
  }
}
