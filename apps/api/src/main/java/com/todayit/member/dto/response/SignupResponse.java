package com.todayit.member.dto.response;

import com.todayit.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * 회원가입 성공 응답입니다.
 *
 * @param memberId 회원 식별자
 * @param email 회원 이메일
 * @param nickname 회원 닉네임
 * @param coupleConnected 커플 연결 여부
 * @param profileImage 프로필 이미지
 * @param createdAt 회원 생성 시각
 */
@Schema(description = "회원가입 성공 응답 데이터")
public record SignupResponse(
    @Schema(description = "생성된 회원 식별자입니다.", example = "123e4567-e89b-12d3-a456-426614174000")
        String memberId,
    @Schema(description = "가입한 이메일입니다.", example = "user@example.com") String email,
    @Schema(description = "회원 닉네임입니다.", example = "오늘이") String nickname,
    @Schema(
            description = "프로필 이미지 참조값입니다. 현재 기본값이 제공되며 직접 이미지 업로드는 미지원입니다.",
            example = "/images/default-profile.png")
        String profileImage,
    @Schema(description = "커플 연결 여부입니다.", example = "false") boolean coupleConnected,
    @Schema(description = "회원 생성 시각입니다. ISO-8601 형식입니다.", example = "2026-10-11T09:00:00+09:00")
        OffsetDateTime createdAt) {

  /**
   * 저장된 회원 정보를 회원가입 응답으로 변환합니다.
   *
   * @param member 저장된 회원
   * @return 회원가입 응답
   */
  public static SignupResponse from(Member member) {
    return new SignupResponse(
        member.getId(),
        member.getEmail(),
        member.getNickname(),
        member.getProfileImage(),
        false,
        member.getCreatedAt());
  }
}
