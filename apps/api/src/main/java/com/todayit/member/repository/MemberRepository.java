package com.todayit.member.repository;

import com.todayit.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 회원 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface MemberRepository extends JpaRepository<Member, String> {

  /**
   * 이메일로 회원을 조회합니다.
   *
   * @param email 조회할 이메일
   * @return 해당 이메일의 회원, 없으면 empty
   */
  Optional<Member> findByEmail(String email);

  /**
   * 회원의 권한 이름을 조회합니다.
   *
   * @param memberId 회원 식별자
   * @return 회원 권한 이름
   */
  @Query(
      value =
          """
                  SELECT r.name
                  FROM member_roles mr
                  JOIN roles r ON r.roles_id = mr.roles_id
                  WHERE mr.member_id = :memberId
                    AND r.deleted_at IS NULL
                  """,
      nativeQuery = true)
  Optional<String> findRoleNameByMemberId(@Param("memberId") String memberId);
}
