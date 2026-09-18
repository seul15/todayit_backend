package com.todayit.member.repository;

import com.todayit.member.entity.Member;
import com.todayit.member.entity.MemberProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
