package com.todayit.member.util;

import java.util.Locale;

/** 이메일 비교와 저장에 사용할 정규화 기능을 제공합니다. */
public final class EmailNormalizer {

  private EmailNormalizer() {}

  /**
   * 이메일 앞뒤 공백을 제거하고 영문을 소문자로 변환합니다.
   *
   * @param email 정규화할 이메일
   * @return 정규화된 이메일
   */
  public static String normalize(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
