package com.todayit.common.file.validation;

import java.util.Locale;

/** 허용된 파일 확장자와 대응하는 MIME입니다. */
public enum FileExtension {
  /** jpg 확장자를 사용하는 JPEG 이미지입니다. */
  JPG("image/jpeg"),
  /** jpeg 확장자를 사용하는 JPEG 이미지입니다. */
  JPEG("image/jpeg"),
  /** PNG 이미지입니다. */
  PNG("image/png"),
  /** WebP 이미지입니다. */
  WEBP("image/webp"),
  /** PDF 문서입니다. */
  PDF("application/pdf");

  private final String mimeType;

  FileExtension(String mimeType) {
    this.mimeType = mimeType;
  }

  /**
   * 대응하는 MIME을 반환합니다.
   *
   * @return MIME 문자열
   */
  public String mimeType() {
    return mimeType;
  }

  /**
   * 검증에 사용할 소문자 확장자를 반환합니다.
   *
   * @return 확장자
   */
  public String value() {
    return name().toLowerCase(Locale.ROOT);
  }
}
