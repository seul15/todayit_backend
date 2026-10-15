package com.todayit.member.entity;

/** 금칙어 문자열의 일치 방식을 나타냅니다. */
public enum BlocklistMatchType {
  /** 입력값 전체가 금칙어와 정확히 일치하는 경우입니다. */
  EXACT,

  /** 입력값에 금칙어가 포함된 경우입니다. */
  CONTAINS
}
