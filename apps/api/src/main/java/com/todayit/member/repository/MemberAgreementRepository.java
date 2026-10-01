package com.todayit.member.repository;

import com.todayit.member.entity.MemberAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 회원의 약관 동의 정보를 저장하는 JPA Repository입니다. */
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Integer> {
  /**
   * 현재 적용 중인 필수 약관 식별자를 조회합니다.
   *
   * @return 필수 약관 식별자 목록
   */
  @Query(
      value =
          """
                    SELECT agreement_id
                    FROM agreement
                    WHERE is_required = TRUE
                      AND is_deleted = FALSE
                    """,
      nativeQuery = true)
  List<Integer> findRequiredAgreementIds();
}
