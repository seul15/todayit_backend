package com.todayit.member.entity;

import com.todayit.theme.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 로그인에 필요한 회원 정보를 나타냅니다. */
@Entity
@Table(name = "member")
public class Member {
  @Id
  @Column(name = "member_id", length = 36)
  private String id;

  // LOCAL 회원만 이메일 저장, 소셜 회원은 null
  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "password", length = 100)
  private String password;

  @Column(name = "nickname", nullable = false, length = 50)
  private String nickname;

  @Column(name = "nickname_updated_at")
  private LocalDateTime nicknameUpdatedAt;

  // 회원이 선택한 선호 지역 ID (region 테이블 참조)
  @Column(name = "preferred_region")
  private Integer preferredRegionId;

  // PostgreSQL 문자열 배열에 선호 테마 코드 저장
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "preferred_theme", columnDefinition = "varchar(20)[]")
  private String[] preferredThemeCodes;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private MemberProvider provider;

  // Google·Kakao에서 발급한 사용자 고유 식별자
  @Column(name = "provider_user_id", length = 255)
  private String providerUserId;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "profile_image", nullable = false, length = 2048)
  private String profileImage = DEFAULT_PROFILE_IMAGE;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected Member() {}

  private Member(
      String id,
      String email,
      String password,
      MemberProvider provider,
      String nickname,
      boolean active,
      LocalDateTime createdAt,
      String providerUserId) {

    this.id = id;
    // LOCAL 회원만 이메일을 저장
    // Google·Kakao 회원은 고유 ID로 식별
    this.email = provider == MemberProvider.LOCAL ? email : null;
    this.password = password;
    this.provider = provider;
    this.nickname = nickname;
    this.active = active;
    this.createdAt = createdAt;

    // Google 등 소셜 서비스에서 발급한 사용자 고유 식별자
    this.providerUserId = providerUserId;
  }

  // 모든 회원이 공통으로 사용하는 기본 프로필 이미지
  private static final String DEFAULT_PROFILE_IMAGE =
      "https://res.cloudinary.com/xmb52jrp/image/upload/v1793191339/default_profile.png";

  /**
   * 회원의 프로필 이미지와 수정 시각을 변경합니다.
   *
   * @param profileImage 변경할 이미지 URL
   * @param updatedAt 변경 시각
   */
  public void changeProfileImage(String profileImage, LocalDateTime updatedAt) {
    this.profileImage = profileImage;
    this.updatedAt = updatedAt;
  }

  /**
   * 회원의 프로필 이미지를 기본 이미지로 복원합니다.
   *
   * @param updatedAt 변경 시각
   */
  public void resetProfileImage(LocalDateTime updatedAt) {
    changeProfileImage(DEFAULT_PROFILE_IMAGE, updatedAt);
  }

  /**
   * 회원의 닉네임과 마지막 변경 시각을 갱신합니다.
   *
   * @param nickname 변경할 닉네임
   * @param updatedAt 변경 시각
   */
  public void changeNickname(String nickname, LocalDateTime updatedAt) {
    this.nickname = nickname;
    this.nicknameUpdatedAt = updatedAt;
    this.updatedAt = updatedAt;
  }

  /**
   * 회원의 선호 지역을 변경합니다.
   *
   * @param regionId 선택한 지역 식별자, null이면 설정 해제
   * @param updatedAt 변경 시각
   */
  public void changePreferredRegion(Integer regionId, LocalDateTime updatedAt) {
    this.preferredRegionId = regionId;
    this.updatedAt = updatedAt;
  }

  /**
   * 회원의 선호 테마를 변경합니다.
   *
   * @param themes 변경할 선호 테마 목록, null이면 설정 해제
   * @param updatedAt 변경 시각
   */
  public void changePreferredThemes(List<Theme> themes, LocalDateTime updatedAt) {
    this.preferredThemeCodes =
        themes == null ? null : themes.stream().map(Theme::name).toArray(String[]::new);
    this.updatedAt = updatedAt;
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
        LocalDateTime.now(),
        null);
  }

  /**
   * Google 인증을 완료한 사용자의 회원 정보를 생성합니다.
   *
   * <p>Google 회원은 자체 비밀번호를 저장하지 않고, Google 계정 고유 ID를 provider_user_id에 저장합니다.
   *
   * @param providerUserId Google 계정 고유 식별자(sub)
   * @param nickname 사용자가 등록한 닉네임
   * @return 생성된 Google 회원
   */
  public static Member createGoogle(String providerUserId, String nickname) {

    return new Member(
        UUID.randomUUID().toString(),
        null,
        null,
        MemberProvider.GOOGLE,
        nickname,
        true,
        LocalDateTime.now(),
        providerUserId);
  }

  /**
   * Kakao 인증을 완료한 사용자의 회원 정보를 생성합니다.
   *
   * <p>Kakao 회원은 자체 비밀번호를 저장하지 않으며, 이메일이 없어도 Kakao 계정 고유 ID로 식별합니다.
   *
   * @param providerUserId Kakao 계정 고유 식별자
   * @param nickname 사용자가 등록한 닉네임
   * @return 생성된 Kakao 회원
   */
  public static Member createKakao(String providerUserId, String nickname) {

    return new Member(
        UUID.randomUUID().toString(),
        null,
        null,
        MemberProvider.KAKAO,
        nickname,
        true,
        LocalDateTime.now(),
        providerUserId);
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

  public LocalDateTime getNicknameUpdatedAt() {
    return nicknameUpdatedAt;
  }

  public MemberProvider getProvider() {
    return provider;
  }

  public String getProviderUserId() {
    return providerUserId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isActive() {
    return active;
  }

  public String getProfileImage() {
    return profileImage;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  /**
   * 회원이 선택한 선호 지역 식별자를 반환합니다.
   *
   * @return 선호 지역 식별자, 설정하지 않았으면 null
   */
  public Integer getPreferredRegionId() {
    return preferredRegionId;
  }

  /**
   * 회원이 선택한 선호 테마 목록을 반환합니다.
   *
   * @return 선호 테마 목록, 설정하지 않았으면 빈 목록
   */
  public List<Theme> getPreferredThemes() {
    if (preferredThemeCodes == null) {
      return List.of();
    }

    return Arrays.stream(preferredThemeCodes).map(Theme::valueOf).toList();
  }
}
