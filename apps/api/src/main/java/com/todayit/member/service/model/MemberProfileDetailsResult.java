package com.todayit.member.service.model;

import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.theme.Theme;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 프로필 조회 및 수정 결과입니다.
 *
 * @param memberId 회원 식별자
 * @param email 회원 이메일, 소셜 회원은 null
 * @param nickname 회원 닉네임
 * @param profileImage 등록된 프로필 이미지 URL, 미등록 시 null
 * @param preferredRegions 선호 지역
 * @param preferredThemes 선호 테마 목록
 * @param updatedAt 회원정보 최종 수정 시각
 */
public record MemberProfileDetailsResult(
    String memberId,
    String email,
    String nickname,
    String profileImage,
    List<PreferredRegionCode> preferredRegions,
    List<Theme> preferredThemes,
    LocalDateTime updatedAt) {

  /** 선호 지역과 테마 목록을 불변으로 보관합니다. */
  public MemberProfileDetailsResult {
    preferredRegions = List.copyOf(preferredRegions);
    preferredThemes = List.copyOf(preferredThemes);
  }
}
