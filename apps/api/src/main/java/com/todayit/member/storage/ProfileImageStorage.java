package com.todayit.member.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 이미지 저장소의 공통 인터페이스입니다.
 *
 * <p>실제 이미지 저장 방식과 관계없이 업로드, 삭제, 접근 URL 생성 기능을 정의합니다.
 */
public interface ProfileImageStorage {

  /**
   * 프로필 이미지를 저장합니다.
   *
   * @param image 저장할 이미지 파일
   * @return 저장된 이미지의 참조값
   */
  String upload(MultipartFile image);

  /**
   * 저장된 프로필 이미지를 삭제합니다.
   *
   * @param imageReference 삭제할 이미지의 참조값
   */
  void delete(String imageReference);

  /**
   * 이미지 참조값을 접근 가능한 URL로 변환합니다.
   *
   * @param imageReference 이미지 참조값
   * @return 이미지 접근 URL
   */
  String resolveUrl(String imageReference);
}
