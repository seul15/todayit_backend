package com.todayit.member.dto.response;

import com.todayit.member.entity.PreferredRegionCode;
import com.todayit.member.service.model.MemberProfileDetailsResult;
import com.todayit.theme.Theme;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 로그인한 회원 본인의 프로필 상세 조회 응답입니다.
 *
 * @param memberId 회원 식별자
 * @param email 회원 이메일, 소셜 회원은 null
 * @param nickname 닉네임
 * @param profileImage 프로필 이미지 URL, 기본 이미지 상태라면 null
 * @param preferredRegions 선호 지역 목록
 * @param preferredThemes 선호 테마 목록
 * @param updatedAt 회원정보 최종 수정 시각
 */
@Schema(description = "회원 프로필 상세 조회 응답")
public record MemberProfileDetailResponse(
    @Schema(description = "회원 식별자") String memberId,
    @Schema(description = "회원 이메일, 소셜 회원은 null") String email,
    @Schema(description = "회원 닉네임") String nickname,
    @Schema(description = "프로필 이미지 URL, 기본 이미지 상태라면 null") String profileImage,
    @Schema(description = "선호 지역 목록") List<PreferredRegionCode> preferredRegions,
    @Schema(description = "선호 테마 코드 목록") List<Theme> preferredThemes,
    @Schema(description = "회원정보 최종 수정 시각") LocalDateTime updatedAt) {

  /**
   * 서비스 조회 결과를 API 응답으로 변환합니다.
   *
   * @param result 회원 프로필 조회 결과
   * @return 회원 프로필 상세 조회 응답
   */
  public static MemberProfileDetailResponse from(MemberProfileDetailsResult result) {
    return new MemberProfileDetailResponse(
        result.memberId(),
        result.email(),
        result.nickname(),
        result.profileImage(),
        result.preferredRegions(),
        result.preferredThemes(),
        result.updatedAt());
  }
}
