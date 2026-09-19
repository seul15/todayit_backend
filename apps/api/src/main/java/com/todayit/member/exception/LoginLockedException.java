package com.todayit.member.exception;

import com.todayit.common.exception.BusinessException;
import java.util.Map;

/** 로그인 실패 횟수 초과로 계정이 잠긴 경우 발생하는 예외입니다. */
public class LoginLockedException extends BusinessException {

  /** 로그인 잠금 예외를 생성합니다. */
  public LoginLockedException() {
    super(MemberErrorCode.LOGIN_LOCKED, Map.of("failureCount", 5L, "maxFailureCount", 5));
  }
}
