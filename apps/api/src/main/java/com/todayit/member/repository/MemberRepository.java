package com.todayit.member.repository;

import com.todayit.member.entity.Member;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface MemberRepository extends JpaRepository<Member, String> {

  /**
   * 이메일로 회원을 조회합니다.
   *
   * @param email 조회할 이메일
   * @return 해당 이메일의 회원, 없으면 empty
   */
  Optional<Member> findByEmail(String email);
}
