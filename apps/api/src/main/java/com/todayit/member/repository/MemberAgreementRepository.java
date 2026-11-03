package com.todayit.member.repository;

import com.todayit.member.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 회원의 약관 동의 정보를 저장하는 JPA Repository입니다. */
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Integer> {

  /**
   * 회원이 현재 적용 중인 약관에 동의했는지 확인합니다.
   *
   * @param memberId 회원 식별자
   * @param agreementCode 확인할 약관 코드
   * @return 해당 약관에 동의했으면 true, 동의하지 않았으면 false
   */
  @Query(
      value =
          """
                    SELECT EXISTS (
                        SELECT 1
                        FROM member_agreement ma
                        JOIN agreement a ON a.agreement_id = ma.agreement_id
                        WHERE ma.member_id = :memberId
                          AND a.code = :agreementCode
                          AND ma.is_agreement = TRUE
                          AND ma.is_deleted = FALSE
                          AND a.is_deleted = FALSE
                    )
                    """,
      nativeQuery = true)
  boolean existsAgreedActiveAgreement(
      @Param("memberId") String memberId, @Param("agreementCode") String agreementCode);
}
