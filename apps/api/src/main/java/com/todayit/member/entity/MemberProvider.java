package com.todayit.member.entity;

/** 회원의 로그인 제공자를 나타냅니다. */
public enum MemberProvider {
  /** 이메일과 비밀번호를 사용하는 자체 로그인입니다. */
  LOCAL,

  /** Google 계정을 사용하는 소셜 로그인입니다. */
  GOOGLE,

  /** Kakao 계정을 사용하는 소셜 로그인입니다. */
  KAKAO
}
