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
    @Schema(description = "회원 식별자") String memberId,
    @Schema(description = "프로필 이미지 URL") String profileImage,
    @Schema(description = "회원정보 수정 시각") LocalDateTime updatedAt) {

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
