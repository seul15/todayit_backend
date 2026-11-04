package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberAgreementRepository;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.MemberProfileResult;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원 프로필 이미지 변경을 처리하는 Command Service입니다. */
@Service
public class MemberProfileService {
  private static final String PROFILE_IMAGE_AGREEMENT_CODE = "PROFILE_IMAGE";

  private final MemberRepository memberRepository;
  private final MemberAgreementRepository memberAgreementRepository;

  /**
   * 회원 프로필 변경에 필요한 Repository를 받습니다.
   *
   * @param memberRepository 회원 Repository
   * @param memberAgreementRepository 회원 약관 동의 Repository
   */
  public MemberProfileService(
      MemberRepository memberRepository, MemberAgreementRepository memberAgreementRepository) {
    this.memberRepository = memberRepository;
    this.memberAgreementRepository = memberAgreementRepository;
  }

  /**
   * 회원의 프로필 이미지 URL과 수정 시각을 변경합니다.
   *
   * <p>프로필 이미지 업로드의 후속 DB 변경 작업에서 호출하면 파일 메타데이터 등록과 동일한 트랜잭션에 참여합니다. 이 메서드에서는 외부 저장소를 호출하지 않습니다.
   *
   * @param memberId 변경할 회원 식별자
   * @param profileImage 업로드가 완료된 프로필 이미지 URL
   * @return 변경된 회원 프로필 정보
   * @throws BusinessException 회원이 존재하지 않거나 비활성 상태인 경우
   */
  @Transactional
  public MemberProfileResult changeProfileImage(String memberId, String profileImage) {
    // 변경 시점에도 회원의 활성 상태를 검증
    Member member =
        memberRepository
            .findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

    // 회원 프로필 이미지와 수정 시각 변경
    LocalDateTime updatedAt = LocalDateTime.now(Clock.systemUTC());
    member.changeProfileImage(profileImage, updatedAt);

    // JPA 변경 감지로 트랜잭션 커밋 시 UPDATE
    return new MemberProfileResult(member.getId(), member.getProfileImage(), member.getUpdatedAt());
  }

  /**
   * 프로필 이미지 등록을 위한 약관 동의 여부를 확인합니다.
   *
   * @param memberId 회원 식별자
   * @throws BusinessException 약관에 동의하지 않은 경우
   */
  @Transactional(readOnly = true)
  public void validateProfileImageConsent(String memberId) {
    boolean agreed =
        memberAgreementRepository.existsAgreedActiveAgreement(
            memberId, PROFILE_IMAGE_AGREEMENT_CODE);

    if (!agreed) {
      throw new BusinessException(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED);
    }
  }
}
