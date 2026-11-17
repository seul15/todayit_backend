package com.todayit.member.repository;

import com.todayit.course.entity.Region;
import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 회원 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface MemberRepository extends JpaRepository<Member, String> {

  /**
   * 이메일과 가입 방식으로 회원을 조회합니다.
   *
   * @param email 조회할 이메일
   * @param provider 가입 방식
   * @return 해당 이메일의 회원, 없으면 empty
   */
  Optional<Member> findByEmailAndProvider(String email, MemberProvider provider);

  /**
   * 이메일과 가입 방식에 해당하는 회원이 존재하는지 확인합니다.
   *
   * @param email 확인할 이메일
   * @param provider 가입 방식
   * @return 회원이 존재하면 true
   */
  boolean existsByEmailAndProvider(String email, MemberProvider provider);

  /**
   * 회원의 권한 목록을 조회합니다.
   *
   * @param memberId 회원 식별자
   * @return 회원 권한 목록
   */
  @Query(
      value =
          """
                  SELECT r.name
                  FROM member_roles mr
                  JOIN roles r ON r.roles_id = mr.roles_id
                  WHERE mr.member_id = :memberId
                    AND r.deleted_at IS NULL
                  ORDER BY r.name
                  """,
      nativeQuery = true)
  List<String> findRoleNamesByMemberId(@Param("memberId") String memberId);

  /**
   * 로그인 제공자와 해당 제공자의 사용자 고유 식별자로 회원을 조회합니다.
   *
   * @param provider 로그인 제공자
   * @param providerUserId 제공자가 발급한 사용자 고유 식별자
   * @return 일치하는 회원, 없으면 empty
   */
  Optional<Member> findByProviderAndProviderUserId(MemberProvider provider, String providerUserId);

  /**
   * 영문 대소문자와 앞뒤 공백을 제외하고 동일한 닉네임이 존재하는지 확인합니다.
   *
   * @param nickname 확인할 닉네임
   * @return 동일한 닉네임이 존재하면 true
   */
  @Query(
      value =
          """
                  SELECT EXISTS (
                      SELECT 1
                      FROM member
                      WHERE LOWER(BTRIM(nickname)) = LOWER(BTRIM(:nickname))
                  )
                  """,
      nativeQuery = true)
  boolean existsByNormalizedNickname(@Param("nickname") String nickname);

  /**
   * 회원정보 변경을 위해 해당 회원 행을 잠그고 조회합니다.
   *
   * @param memberId 회원 식별자
   * @return 회원 정보, 없으면 empty
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT m FROM Member m WHERE m.id = :memberId")
  Optional<Member> findByIdForUpdate(@Param("memberId") String memberId);

  /**
   * 프로필 이미지 URL과 수정 시각을 변경합니다.
   *
   * @param memberId 회원 식별자
   * @param profileImage 이미지 URL
   * @param updatedAt 수정 시각
   * @return 변경된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query(
      """
    UPDATE Member m
    SET m.profileImage = :profileImage,
        m.updatedAt = :updatedAt
    WHERE m.id = :memberId
    """)
  int updateProfileImage(
      @Param("memberId") String memberId,
      @Param("profileImage") String profileImage,
      @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 프로필 이미지를 DB 기본값으로 복원합니다.
   *
   * @param memberId 회원 식별자
   * @param updatedAt 수정 시각
   * @return 변경된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
        UPDATE member
        SET profile_image = DEFAULT,
            updated_at = :updatedAt
        WHERE member_id = :memberId
        """,
      nativeQuery = true)
  int resetProfileImage(
      @Param("memberId") String memberId, @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 닉네임과 마지막 변경 시각을 갱신합니다.
   *
   * @param memberId 회원 식별자
   * @param nickname 변경할 닉네임
   * @param updatedAt 변경 시각
   * @return 변경된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query(
      """
    UPDATE Member m
    SET m.nickname = :nickname,
        m.nicknameUpdatedAt = :updatedAt,
        m.updatedAt = :updatedAt
    WHERE m.id = :memberId
    """)
  int updateNickname(
      @Param("memberId") String memberId,
      @Param("nickname") String nickname,
      @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 선호 지역과 수정 시각을 변경합니다.
   *
   * @param memberId 회원 식별자
   * @param region 선택한 지역, 해제 시 null
   * @param updatedAt 수정 시각
   * @return 변경된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query(
      """
    UPDATE Member m
    SET m.preferredRegion = :region,
        m.updatedAt = :updatedAt
    WHERE m.id = :memberId
    """)
  int updatePreferredRegion(
      @Param("memberId") String memberId,
      @Param("region") Region region,
      @Param("updatedAt") LocalDateTime updatedAt);

  /**
   * 선호 테마 배열과 수정 시각을 변경합니다.
   *
   * @param memberId 회원 식별자
   * @param themeCodes 쉼표로 구분한 테마 코드, 빈 문자열이면 전체 해제
   * @param updatedAt 수정 시각
   * @return 변경된 행 수
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
        UPDATE member
        SET preferred_theme =
            CAST(string_to_array(:themeCodes, ',') AS varchar(20)[]),
            updated_at = :updatedAt
        WHERE member_id = :memberId
        """,
      nativeQuery = true)
  int updatePreferredThemes(
      @Param("memberId") String memberId,
      @Param("themeCodes") String themeCodes,
      @Param("updatedAt") LocalDateTime updatedAt);
}
