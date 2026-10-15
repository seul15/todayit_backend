package com.todayit.common.file.service.model;

import com.todayit.common.file.entity.File.FileSnapshot;
import com.todayit.common.file.entity.FileRelatedType;
import java.time.LocalDateTime;

/**
 * 파일 메타데이터 조회 결과입니다. URL 발급 전 호출자가 대상 접근 권한을 검증해야 합니다.
 *
 * @param fileId 파일 식별자
 * @param relatedType 사용처
 * @param relatedId 대상 식별자
 * @param originalName 원본명
 * @param storedName 저장 파일명
 * @param mimeType 검증한 MIME
 * @param sizeBytes 바이트 크기
 * @param createdAt 생성 일시
 */
public record FileResult(
    long fileId,
    FileRelatedType relatedType,
    String relatedId,
    String originalName,
    String storedName,
    String mimeType,
    long sizeBytes,
    LocalDateTime createdAt) {

  /**
   * 파일 조회 정보를 서비스 결과로 변환합니다.
   *
   * @param snapshot 파일 조회 정보
   * @return 불변 메타데이터
   */
  public static FileResult from(FileSnapshot snapshot) {
    return new FileResult(
        snapshot.fileId(),
        snapshot.relatedType(),
        snapshot.relatedId(),
        snapshot.originalName(),
        snapshot.storedName(),
        snapshot.mimeType(),
        snapshot.sizeBytes(),
        snapshot.createdAt());
  }
}
