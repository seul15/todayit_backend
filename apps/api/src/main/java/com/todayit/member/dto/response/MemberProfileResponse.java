package com.todayit.member.dto.response;

import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.theme.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 프로필 수정 응답입니다.
 *
 * @param memberId 회원 식별자
 * @param nickname 닉네임
 * @param preferredRegionId 선호 지역 식별자
 * @param preferredThemes 선호 테마
 * @param updatedAt 회원정보 수정 시각
 */
@Schema(description = "회원 프로필 수정 응답")
public record MemberProfileResponse(
    @Schema(description = "회원 식별자") String memberId,
    @Schema(description = "닉네임") String nickname,
    @Schema(description = "선호 지역 식별자") Integer preferredRegionId,
    @Schema(description = "선호 테마 코드 목록") List<Theme> preferredThemes,
    @Schema(description = "회원정보 수정 시각") LocalDateTime updatedAt) {

  /**
   * 서비스 결과를 API 응답으로 변환합니다.
   *
   * @param result 회원 프로필 수정 결과
   * @return API 응답
   */
  public static MemberProfileResponse from(MemberProfileDetailsResult result) {
    return new MemberProfileResponse(
        result.memberId(),
        result.nickname(),
        result.preferredRegionId(),
        result.preferredThemes(),
        result.updatedAt());
  }
}
