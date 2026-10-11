package com.todayit.member.repository;

import com.todayit.member.entity.Agreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 서비스 약관 정보를 조회하는 Repository입니다. */
public interface AgreementRepository extends JpaRepository<Agreement, Integer> {

  /**
   * 삭제되지 않은 현재 약관을 조회합니다.
   *
   * @return 현재 적용 중인 약관 목록
   */
  List<Agreement> findByDeletedFalse();
}
