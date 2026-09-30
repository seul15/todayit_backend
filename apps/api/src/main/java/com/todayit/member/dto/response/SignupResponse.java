package com.todayit.member.dto.response;

import com.todayit.member.entity.Member;
import java.time.OffsetDateTime;

/**
 * 회원가입 성공 응답입니다.
 *
 * @param memberId 회원 식별자
 * @param email 회원 이메일
 * @param nickname 회원 닉네임
 * @param coupleConnected 커플 연결 여부
 * @param createdAt 회원 생성 시각
 */
public record SignupResponse(
    String memberId,
    String email,
    String nickname,
    boolean coupleConnected,
    OffsetDateTime createdAt) {

  /**
   * 저장된 회원 정보를 회원가입 응답으로 변환합니다.
   *
   * @param member 저장된 회원
   * @return 회원가입 응답
   */
  public static SignupResponse from(Member member) {
    return new SignupResponse(
        member.getId(), member.getEmail(), member.getNickname(), false, member.getCreatedAt());
  }
}
