package com.todayit.member.repository;

import com.todayit.member.entity.MemberRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 회원 권한 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface MemberRoleRepository extends JpaRepository<MemberRole, Integer> {

  /**
   * 권한 이름으로 권한 식별자를 조회합니다.
   *
   * @param roleName 조회할 권한 이름
   * @return 권한 식별자
   */
  @Query(
      value =
          """
                    SELECT roles_id
                    FROM roles
                    WHERE name = :roleName
                      AND deleted_at IS NULL
                    """,
      nativeQuery = true)
  Optional<Integer> findRoleIdByName(@Param("roleName") String roleName);
}
