package com.todayit.common.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 외부 저장소 파일의 메타데이터입니다. */
@Entity
@Table(name = "file")
public class File {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "file_id")
  private Long fileId;

  @Enumerated(EnumType.STRING)
  @Column(name = "related_type", nullable = false, length = 30)
  private FileRelatedType relatedType;

  @Column(name = "related_id", nullable = false, length = 36)
  private String relatedId;

  @Column(name = "original_name", nullable = false, length = 255)
  private String originalName;

  @Column(name = "stored_name", nullable = false, length = 1024)
  private String storedName;

  @Column(name = "mime_type", nullable = false, length = 100)
  private String mimeType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** JPA가 엔티티를 복원할 때 사용하는 기본 생성자입니다. */
  protected File() {}

  /**
   * 업로드 완료된 파일 메타데이터를 만듭니다.
   *
   * @param relatedType 사용처
   * @param relatedId 대상 식별자
   * @param originalName 원본명
   * @param storedName 저장 파일명
   * @param mimeType 검증한 MIME 유형
   * @param sizeBytes 바이트 크기
   */
  public File(
      FileRelatedType relatedType,
      String relatedId,
      String originalName,
      String storedName,
      String mimeType,
      long sizeBytes) {
    this.relatedType = relatedType;
    this.relatedId = relatedId;
    this.originalName = originalName;
    this.storedName = storedName;
    this.mimeType = mimeType;
    this.sizeBytes = sizeBytes;
    this.createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
  }

  /**
   * 파일 메타데이터를 하나의 불변 값으로 반환합니다.
   *
   * @return 파일 조회 정보
   */
  public FileSnapshot getSnapshot() {
    return new FileSnapshot(
        fileId, relatedType, relatedId, originalName, storedName, mimeType, sizeBytes, createdAt);
  }

  /**
   * 파일 조회와 외부 저장소 작업에 필요한 메타데이터입니다.
   *
   * @param fileId 파일 식별자. 저장 전이면 null
   * @param relatedType 파일 사용처
   * @param relatedId 연결 대상 식별자
   * @param originalName 원본 파일명
   * @param storedName 저장 파일명
   * @param mimeType MIME 유형
   * @param sizeBytes 파일 크기(바이트)
   * @param createdAt 파일 생성 시각
   */
  public record FileSnapshot(
      Long fileId,
      FileRelatedType relatedType,
      String relatedId,
      String originalName,
      String storedName,
      String mimeType,
      long sizeBytes,
      LocalDateTime createdAt) {}
}
