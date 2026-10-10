package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** 로그인에 필요한 회원 정보를 나타냅니다. */
@Entity
@Table(name = "member")
public class Member {

  private static final String DEFAULT_PROFILE_IMAGE = "/images/default-profile.png";

  @Id
  @Column(name = "member_id", length = 36)
  private String id;

  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Column(name = "password", length = 100)
  private String password;

  @Column(name = "nickname", nullable = false, length = 50)
  private String nickname;

  @Column(name = "profile_image", nullable = false, length = 2048)
  private String profileImage;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private MemberProvider provider;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Member() {}

  private Member(
      String id,
      String email,
      String password,
      MemberProvider provider,
      String nickname,
      boolean active,
      OffsetDateTime createdAt) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.provider = provider;
    this.nickname = nickname;
    this.profileImage = DEFAULT_PROFILE_IMAGE;
    this.active = active;
    this.createdAt = createdAt;
  }

  /**
   * 이메일과 비밀번호를 사용하는 로컬 회원을 생성합니다.
   *
   * @param email 회원 이메일
   * @param encodedPassword 암호화된 비밀번호
   * @param nickname 회원 닉네임
   * @return 생성된 로컬 회원
   */
  public static Member createLocal(String email, String encodedPassword, String nickname) {
    return new Member(
        UUID.randomUUID().toString(),
        email,
        encodedPassword,
        MemberProvider.LOCAL,
        nickname,
        true,
        OffsetDateTime.now());
  }

  public String getId() {
    return id;
  }

  public String getPassword() {
    return password;
  }

  public String getEmail() {
    return email;
  }

  public String getNickname() {
    return nickname;
  }

  /**
   * 회원의 프로필 이미지 참조값을 반환합니다.
   *
   * @return 프로필 이미지 참조값
   */
  public String getProfileImage() {
    return profileImage;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isActive() {
    return active;
  }
}
