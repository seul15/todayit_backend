package com.todayit.common.file.exception;

import com.todayit.common.exception.ErrorCode;
import com.todayit.common.exception.ErrorStatus;

/** 파일 입력 검증과 저장소 작업에서 사용하는 오류 코드입니다. */
public enum FileErrorCode implements ErrorCode {
  INVALID_TARGET("FILE_INVALID_TARGET", "유효한 파일 연결 대상이 필요합니다.", ErrorStatus.BAD_REQUEST),
  INVALID_FILES("FILE_INVALID_FILES", "업로드할 파일과 사용처를 지정해 주세요.", ErrorStatus.BAD_REQUEST),
  INVALID_FILE_IDS("FILE_INVALID_IDS", "삭제할 파일 식별자가 유효하지 않습니다.", ErrorStatus.BAD_REQUEST),
  INVALID_NAME("FILE_INVALID_NAME", "유효한 원본 파일명이 필요합니다.", ErrorStatus.BAD_REQUEST),
  INVALID_SIZE("FILE_INVALID_SIZE", "파일 크기가 허용 범위를 벗어났습니다.", ErrorStatus.BAD_REQUEST),
  UNSUPPORTED_EXTENSION(
      "FILE_UNSUPPORTED_EXTENSION", "허용 확장자는 jpg/jpeg/png/webp/pdf입니다.", ErrorStatus.BAD_REQUEST),
  PROFILE_IMAGE_REQUIRED(
      "FILE_PROFILE_IMAGE_REQUIRED", "프로필에는 이미지만 업로드할 수 있습니다.", ErrorStatus.BAD_REQUEST),
  PROFILE_LIMIT_EXCEEDED(
      "FILE_PROFILE_LIMIT_EXCEEDED", "프로필 이미지는 하나만 업로드할 수 있습니다.", ErrorStatus.BAD_REQUEST),
  INQUIRY_LIMIT_EXCEEDED(
      "FILE_INQUIRY_LIMIT_EXCEEDED",
      "문의 첨부파일 개수 또는 합계 용량이 허용 범위를 초과했습니다.",
      ErrorStatus.BAD_REQUEST),
  MIME_MISMATCH("FILE_MIME_MISMATCH", "확장자와 MIME이 일치하지 않습니다.", ErrorStatus.BAD_REQUEST),
  CONTENT_MISMATCH(
      "FILE_CONTENT_MISMATCH", "파일의 실제 형식이 확장자 및 MIME과 일치하지 않습니다.", ErrorStatus.BAD_REQUEST),
  NOT_FOUND("FILE_NOT_FOUND", "대상에 연결된 파일을 찾을 수 없습니다.", ErrorStatus.NOT_FOUND),
  READ_FAILED("FILE_READ_FAILED", "업로드 파일을 읽을 수 없습니다.", ErrorStatus.INTERNAL_SERVER_ERROR),
  STORAGE_FAILED("FILE_STORAGE_FAILED", "파일 저장소 작업에 실패했습니다.", ErrorStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final ErrorStatus status;

  FileErrorCode(String code, String message, ErrorStatus status) {
    this.code = code;
    this.message = message;
    this.status = status;
  }

  /**
   * 오류 코드를 반환합니다.
   *
   * @return 오류 코드
   */
  @Override
  public String code() {
    return code;
  }

  /**
   * 오류 메시지를 반환합니다.
   *
   * @return 오류 메시지
   */
  @Override
  public String message() {
    return message;
  }

  /**
   * 업무 오류 상태를 반환합니다.
   *
   * @return 업무 오류 상태
   */
  @Override
  public ErrorStatus status() {
    return status;
  }
}
