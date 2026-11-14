package com.todayit.member.dto.request;

import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.service.command.MemberProfileCommand;
import com.todayit.theme.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(
            description =
                "변경할 닉네임. 2~8자, 한글·영문·숫자만 허용합니다. " + "생략하면 기존 닉네임을 유지하며, 실제 변경 시 90일 제한이 적용됩니다.",
            example = "새닉네임")
        String nickname,
    @Schema(
            description =
                "선호 지역 목록. 지정된 30개 지역 중 최대 1개만 선택할 수 있습니다. "
                    + "각 지역은 sidoCode(시도 2자리), sigunguCode(시군구 3자리)로 구성하며 문자열로 전달합니다. "
                    + "생략 또는 null이면 기존 선택을 유지하고, 빈 배열([])이면 선택을 해제합니다.",
            example = "[{\"sidoCode\":\"11\",\"sigunguCode\":\"440\"}]")
        List<PreferredRegionCode> preferredRegions,
    @Schema(
            description =
                "선호 테마 목록. 복수 선택할 수 있으며 중복은 허용하지 않습니다. "
                    + "생략 또는 null이면 기존 선택을 유지하고, 빈 배열([])이면 선택을 해제합니다. "
                    + "사용 가능 코드: QUIET, ROMANTIC, COZY, EMOTIONAL, LIVELY, "
                    + "UNIQUE, RELAXED, LUXURIOUS, RETRO, NATURE_FRIENDLY.",
            example = "[\"QUIET\",\"ROMANTIC\"]")
        List<Theme> preferredThemes) {
  /**
   * 요청을 서비스 Command로 변환합니다.
   *
   * @return 회원 프로필 수정 Command
   */
  public MemberProfileCommand toCommand() {
    return new MemberProfileCommand(nickname, preferredRegions, preferredThemes);
  }
}
