package com.todayit.member.repository;

import com.todayit.member.entity.BlocklistMatchType;
import com.todayit.member.entity.PasswordBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;

/** 비밀번호 금칙어 정보를 조회하는 JPA Repository입니다. */
public interface PasswordBlocklistRepository extends JpaRepository<PasswordBlocklist, Integer> {

  /**
   * 활성화된 금칙어 중 입력 문자열 및 일치 방식과 동일한 항목이 존재하는지 확인합니다.
   *
   * @param word 확인할 문자열
   * @param matchType 일치 방식
   * @return 금칙어가 존재하면 true
   */
  boolean existsByWordAndMatchTypeAndEnabledTrue(String word, BlocklistMatchType matchType);
}
