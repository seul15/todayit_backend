package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 회원의 약관 동의 정보를 나타냅니다. */
@Entity
@Table(name = "member_agreement")
public class MemberAgreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_agreement_id")
  private Integer id;

  @Column(name = "agreement_id", nullable = false)
  private Integer agreementId;

  @Column(name = "member_id", nullable = false, length = 36)
  private String memberId;

  @Column(name = "is_agreement", nullable = false)
  private boolean agreed;

  protected MemberAgreement() {}

  private MemberAgreement(Integer agreementId, String memberId, boolean agreed) {
    this.agreementId = agreementId;
    this.memberId = memberId;
    this.agreed = agreed;
  }

  /**
   * 회원의 약관 동의 정보를 생성합니다.
   *
   * @param agreementId 약관 식별자
   * @param memberId 회원 식별자
   * @param agreed 약관 동의 여부
   * @return 생성된 회원 약관 동의 정보
   */
  public static MemberAgreement create(Integer agreementId, String memberId, boolean agreed) {
    return new MemberAgreement(agreementId, memberId, agreed);
  }
}
