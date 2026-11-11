package com.todayit.member.dto.response;

import com.todayit.member.service.model.MemberProfileResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 회원 프로필 이미지 등록 및 변경, 삭제 응답입니다.
 *
 * @param memberId 회원 식별자
 * @param profileImage 변경된 프로필 이미지 URL
 * @param updatedAt 회원정보 수정 시각
 */
@Schema(description = "프로필 이미지 등록, 변경 및 삭제 응답")
public record MemberProfileImageResponse(
    @Schema(
            description = "회원 식별자입니다.",
            example = "123e4567-e89b-12d3-a456-426614174000",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String memberId,
    @Schema(
            description = "변경된 프로필 이미지 URL입니다. 이미지 삭제 또는 기본 이미지 상태라면 null을 반환합니다.",
            example = "https://example.com/profile.png",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        String profileImage,
    @Schema(
            description = "회원정보 최종 수정 시각입니다. 수정 이력이 없으면 null일 수 있습니다.",
            example = "2026-11-11T07:00:00",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        LocalDateTime updatedAt) {

  /**
   * 서비스 결과를 API 응답으로 변환합니다.
   *
   * @param result 프로필 이미지 변경 결과
   * @return 프로필 이미지 API 응답
   */
  public static MemberProfileImageResponse from(MemberProfileResult result) {
    return new MemberProfileImageResponse(
        result.memberId(), result.profileImage(), result.updatedAt());
  }
}
