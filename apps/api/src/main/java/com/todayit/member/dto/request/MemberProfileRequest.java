package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.member.service.command.MemberProfileCommand.PreferredRegion;
import com.todayit.theme.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.List;

/**
 * 회원 프로필 수정 요청입니다.
 *
 * @param nickname 변경할 닉네임
 * @param preferredRegion 변경할 선호 지역
 * @param preferredThemes 변경할 선호 테마 목록
 */
@Schema(description = "회원 프로필 수정 요청")
public record MemberProfileRequest(
    @Schema(description = "변경할 닉네임. 생략하면 유지", example = "오늘") String nickname,
    @Schema(description = "변경할 선호 지역. 생략하면 유지") PreferredRegionRequest preferredRegion,
    @Schema(description = "선호 테마 코드 목록. 빈 배열은 선택 해제") List<Theme> preferredThemes) {

  /**
   * 요청에 수정 항목이 있고 테마 목록이 유효한지 검증합니다.
   *
   * @throws InvalidRequestException 수정할 항목이 없거나 입력이 잘못된 경우
   */
  public void validate() {
    if (nickname == null && preferredRegion == null && preferredThemes == null) {
      throw new InvalidRequestException("수정할 회원정보가 없습니다.");
    }

    if (preferredRegion != null) {
      boolean hasSidoCode = preferredRegion.sidoCode() != null;
      boolean hasSigunguCode = preferredRegion.sigunguCode() != null;

      if (hasSidoCode != hasSigunguCode) {
        throw new InvalidRequestException("선호 지역이 올바르지 않습니다.");
      }
    }

    if (preferredThemes != null) {
      if (preferredThemes.contains(null)) {
        throw new InvalidRequestException("선호 테마 목록이 올바르지 않습니다.");
      }

      if (new HashSet<>(preferredThemes).size() != preferredThemes.size()) {
        throw new InvalidRequestException("선호 테마를 중복 선택할 수 없습니다.");
      }
    }
  }

  /**
   * API 요청을 서비스 Command로 변환합니다.
   *
   * @return 회원 프로필 변경 Command
   */
  public MemberProfileCommand toCommand() {
    PreferredRegion region =
        preferredRegion == null
            ? null
            : new PreferredRegion(preferredRegion.sidoCode(), preferredRegion.sigunguCode());

    return new MemberProfileCommand(nickname, region, preferredThemes);
  }

  /**
   * 선호 지역 입력입니다.
   *
   * @param sidoCode 시·도 코드
   * @param sigunguCode 시·군·구 코드
   */
  public record PreferredRegionRequest(String sidoCode, String sigunguCode) {}
}
