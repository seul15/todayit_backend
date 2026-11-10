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
    @Schema(description = "회원 식별자") String memberId,
    @Schema(description = "닉네임") String nickname,
    @Schema(description = "프로필 이미지 URL, 미등록 시 null") String profileImage,
    @Schema(description = "선호 지역") List<PreferredRegionCode> preferredRegions,
    @Schema(description = "선호 테마 코드 목록") List<Theme> preferredThemes,
    @Schema(description = "회원정보 수정 시각") LocalDateTime updatedAt) {

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

  /**
   * 로그인한 회원 본인의 프로필 조회 응답입니다.
   *
   * @param memberId 회원 식별자
   * @param email 회원 이메일, 소셜 회원은 null
   * @param nickname 닉네임
   * @param profileImage 프로필 이미지 URL
   * @param preferredRegions 선호 지역 목록
   * @param preferredThemes 선호 테마 목록
   * @param updatedAt 회원정보 수정 시각
   */
  @Schema(description = "본인 회원정보 조회 응답")
  public record Query(
      String memberId,
      String email,
      String nickname,
      String profileImage,
      List<PreferredRegionCode> preferredRegions,
      List<Theme> preferredThemes,
      LocalDateTime updatedAt) {

    /**
     * 서비스 결과를 회원정보 조회 응답으로 변환합니다.
     *
     * @param result 회원 프로필 정보
     * @return 회원정보 조회 응답
     */
    public static Query from(MemberProfileDetailsResult result) {
      return new Query(
          result.memberId(),
          result.email(),
          result.nickname(),
          result.profileImage(),
          result.preferredRegions(),
          result.preferredThemes(),
          result.updatedAt());
    }
  }
}
