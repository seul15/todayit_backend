package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.Map;

/** 이메일 또는 비밀번호가 올바르지 않은 로그인 요청에서 발생하는 예외입니다. */
public class LoginFailedException extends BusinessException {

  private static final int MAX_FAILURE_COUNT = 5;

  /** 로그인 실패 예외를 생성합니다. */
  public LoginFailedException() {
    super(MemberErrorCode.LOGIN_FAILED, details(null));
  }

  /**
   * 현재 로그인 실패 횟수를 포함한 예외를 생성합니다.
   *
   * @param failureCount 현재 로그인 실패 횟수
   */
  public LoginFailedException(long failureCount) {
    super(MemberErrorCode.LOGIN_FAILED, details(failureCount));
  }

  private static Map<String, Object> details(Long failureCount) {
    Map<String, Object> details = new LinkedHashMap<>();

    details.put("failureCount", failureCount);
    details.put("maxFailureCount", MAX_FAILURE_COUNT);

    return details;
  }
}
