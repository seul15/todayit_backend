package com.todayit.common.file.exception;

/** 제공자의 응답 및 비밀값을 노출하지 않는 저장소 작업 예외입니다. */
public class FileStorageException extends FileException {
  /** 저장소 작업 실패를 나타냅니다. */
  public FileStorageException() {
    super(FileErrorCode.STORAGE_FAILED);
  }
}
