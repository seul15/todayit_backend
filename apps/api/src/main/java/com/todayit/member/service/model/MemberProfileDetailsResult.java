package com.todayit.member.service.model;

import com.todayit.theme.Theme;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 프로필 수정 결과입니다.
 *
 * @param memberId 회원 식별자
 * @param nickname 닉네임
 * @param preferredRegionId 선호 지역 식별자
 * @param preferredThemes 선호 테마 목록
 * @param updatedAt 회원정보 수정 시각
 */
public record MemberProfileDetailsResult(
    String memberId,
    String nickname,
    Integer preferredRegionId,
    List<Theme> preferredThemes,
    LocalDateTime updatedAt) {

  /** 테마 목록을 불변으로 보관합니다. */
  public MemberProfileDetailsResult {
    preferredThemes = List.copyOf(preferredThemes);
  }
}
