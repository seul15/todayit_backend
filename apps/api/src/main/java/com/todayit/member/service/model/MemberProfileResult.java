package com.todayit.member.service.model;

import java.time.LocalDateTime;

/**
 * 회원 프로필 이미지 변경 결과입니다.
 *
 * @param memberId 회원 식별자
 * @param profileImage 변경된 프로필 이미지 URL
 * @param updatedAt 회원정보 수정 시각
 */
public record MemberProfileResult(String memberId, String profileImage, LocalDateTime updatedAt) {}
