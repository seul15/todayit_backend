package com.todayit.common.file.exception;

import com.todayit.common.exception.BusinessException;

/** 파일 오류를 공통 응답 형식으로 전달하는 업무 예외입니다. */
public class FileException extends BusinessException {
  /**
   * 파일 오류 코드로 예외를 생성합니다.
   *
   * @param errorCode 파일 오류 코드
   */
  public FileException(FileErrorCode errorCode) {
    super(errorCode);
  }
}
