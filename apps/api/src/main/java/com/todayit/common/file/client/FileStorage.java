package com.todayit.common.file.client;

import com.todayit.common.file.entity.FileRelatedType;

/** 저장소를 교체할 수 있는 외부 파일 저장 계약입니다. */
public interface FileStorage {
  /**
   * 지정한 키로 파일을 업로드합니다. 기존 파일을 덮어쓰지 않습니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME
   * @param type 사용처
   * @param content 파일 내용
   */
  void upload(String storedName, String mimeType, FileRelatedType type, byte[] content);

  /**
   * 파일을 멱등하게 삭제합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME
   * @param type 사용처
   */
  void delete(String storedName, String mimeType, FileRelatedType type);

  /**
   * 접근 URL을 생성합니다. 호출자가 접근 권한을 먼저 확인해야 합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME
   * @param type 사용처
   * @return 공개 URL 또는 서명 URL
   */
  String accessUrl(String storedName, String mimeType, FileRelatedType type);
}
