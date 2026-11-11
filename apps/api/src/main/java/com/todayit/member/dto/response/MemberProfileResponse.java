package com.todayit.member.dto.response;

import com.todayit.member.entity.PreferredRegionCode;
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
 * @param profileImage 등록된 프로필 이미지 URL, 미등록 시 null
 * @param preferredRegions 선호 지역
 * @param preferredThemes 선호 테마 목록
 * @param updatedAt 회원정보 최종 수정 시각
 */
@Schema(description = "회원 프로필 수정 응답")
public record MemberProfileResponse(
    @Schema(
            description = "회원 식별자입니다.",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String memberId,
    @Schema(description = "회원 닉네임입니다.", example = "오늘", requiredMode = Schema.RequiredMode.REQUIRED)
        String nickname,
    @Schema(
            description = "등록된 프로필 이미지 URL입니다. 기본 이미지 상태라면 null을 반환합니다.",
            example = "https://example.com/profile.png",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        String profileImage,
    @Schema(
            description = "선호 지역 목록입니다. 설정하지 않았으면 빈 배열을 반환하며 최대 1개까지 포함합니다.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<PreferredRegionCode> preferredRegions,
    @Schema(
            description = "선호 테마 코드 목록입니다. 설정하지 않았으면 빈 배열을 반환합니다.",
            example = "[\"QUIET\",\"ROMANTIC\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<Theme> preferredThemes,
    @Schema(
            description = "회원정보 최종 수정 시각입니다. 수정 이력이 없으면 null일 수 있습니다.",
            example = "2026-11-11T07:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        LocalDateTime updatedAt) {
  /**
   * 서비스 결과를 API 응답으로 변환합니다.
   *
   * @param result 회원 프로필 수정 결과
   * @return 회원 프로필 수정 응답
   */
  public static MemberProfileResponse from(MemberProfileDetailsResult result) {
    return new MemberProfileResponse(
        result.memberId(),
        result.nickname(),
        result.profileImage(),
        result.preferredRegions(),
        result.preferredThemes(),
        result.updatedAt());
  }
}
