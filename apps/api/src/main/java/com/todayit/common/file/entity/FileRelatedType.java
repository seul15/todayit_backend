package com.todayit.common.file.entity;

/** 파일을 사용하는 대상과 공개 정책입니다. */
public enum FileRelatedType {
  /** 회원 프로필 이미지입니다. 공개 URL로 제공합니다. */
  MEMBER_PROFILE,
  /** FAQ 첨부 파일입니다. 공개 URL로 제공합니다. */
  FAQ_ATTACHMENT,
  /** 문의 첨부 파일입니다. 접근 권한 확인 후 서명 URL로 제공합니다. */
  INQUIRY_ATTACHMENT;

  /**
   * Cloudinary 제공 방식을 반환합니다.
   *
   * @return 문의 첨부는 authenticated, 그 외는 upload
   */
  public String deliveryType() {
    return this == INQUIRY_ATTACHMENT ? "authenticated" : "upload";
  }
}
