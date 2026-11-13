package com.todayit.member.dto.request;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.theme.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.List;

/**
 * 회원 프로필 수정 요청입니다.
 *
 * @param nickname 변경할 닉네임
 * @param preferredRegions 변경할 선호 지역
 * @param preferredThemes 변경할 선호 테마 목록
 */
@Schema(description = "회원 프로필 수정 요청")
public record MemberProfileRequest(
    @Schema(description = "변경할 닉네임") String nickname,
    @Schema(description = "선호 지역 목록, 지정된 30개 지역 중 최대 1개. 시도/시군구 코드로 구성")
        List<PreferredRegionCode> preferredRegions,
    @Schema(description = "선호 테마 목록") List<Theme> preferredThemes) {

  /**
   * 요청 입력값을 검증합니다.
   *
   * @throws InvalidRequestException 잘못된 입력값이 있는 경우
   */
  public void validate() {
    if (nickname == null && preferredRegions == null && preferredThemes == null) {
      throw new InvalidRequestException("수정할 회원정보가 없습니다.");
    }

    if (preferredRegions != null) {
      if (preferredRegions.size() > 1) {
        throw new InvalidRequestException("선호 지역은 1개만 선택할 수 있습니다.");
      }

      for (PreferredRegionCode region : preferredRegions) {
        if (region == null
            || region.sidoCode() == null
            || region.sigunguCode() == null
            || !region.sidoCode().matches("[0-9]{2}")
            || !region.sigunguCode().matches("[0-9]{3}")) {
          throw new InvalidRequestException("선호 지역 코드가 올바르지 않습니다.");
        }
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
   * 요청을 서비스 Command로 변환합니다.
   *
   * @return 회원 프로필 수정 Command
   */
  public MemberProfileCommand toCommand() {
    return new MemberProfileCommand(nickname, preferredRegions, preferredThemes);
  }
}
