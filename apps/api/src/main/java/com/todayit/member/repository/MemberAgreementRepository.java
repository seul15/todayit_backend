package com.todayit.member.repository;

import com.todayit.member.entity.MemberAgreement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 회원의 약관 동의 정보를 저장하는 JPA Repository입니다. */
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Integer> {

  /**
   * 현재 적용 중인 약관의 식별자, 코드, 필수 여부를 조회합니다.
   *
   * @return 현재 적용 중인 약관 목록
   */
  @Query(
      value =
          """
                  SELECT agreement_id AS "agreementId",
                         code,
                         is_required AS "required"
                  FROM agreement
                  WHERE is_deleted = FALSE
                  """,
      nativeQuery = true)
  List<CurrentAgreement> findCurrentAgreements();

  /** 현재 적용 중인 약관 조회 결과입니다. */
  interface CurrentAgreement {

    /**
     * @return 약관 식별자
     */
    Integer getAgreementId();

    /**
     * @return 약관 코드
     */
    String getCode();

    /**
     * @return 필수 동의 여부
     */
    Boolean getRequired();
  }
}
