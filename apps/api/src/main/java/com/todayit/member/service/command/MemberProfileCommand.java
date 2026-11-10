package com.todayit.member.service.command;

import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.theme.Theme;
import java.util.List;

/**
 * 회원 프로필 변경에 필요한 정보입니다.
 *
 * @param nickname 변경할 닉네임
 * @param preferredRegions 변경할 선호 지역
 * @param preferredThemes 변경할 선호 테마 목록
 */
public record MemberProfileCommand(
    String nickname, List<PreferredRegionCode> preferredRegions, List<Theme> preferredThemes) {

  /** 선택 목록을 방어적으로 복사합니다. */
  public MemberProfileCommand {
    if (preferredRegions != null) {
      preferredRegions = List.copyOf(preferredRegions);
    }
    if (preferredThemes != null) {
      preferredThemes = List.copyOf(preferredThemes);
    }
  }
}
