package com.todayit.member.service.command;

import com.todayit.theme.Theme;
import java.util.List;

/**
 * 회원 프로필 변경에 필요한 정보입니다.
 *
 * @param nickname 변경할 닉네임
 * @param preferredRegion 변경할 선호 지역
 * @param preferredThemes 변경할 선호 테마 목록
 */
public record MemberProfileCommand(
    String nickname, PreferredRegion preferredRegion, List<Theme> preferredThemes) {

  /** 선호 테마 목록을 방어적으로 복사합니다. */
  public MemberProfileCommand {
    if (preferredThemes != null) {
      preferredThemes = List.copyOf(preferredThemes);
    }
  }

  /**
   * 회원이 선택한 지역 정보입니다.
   *
   * @param sidoCode 시·도 코드
   * @param sigunguCode 시·군·구 코드
   */
  public record PreferredRegion(String sidoCode, String sigunguCode) {}
}
