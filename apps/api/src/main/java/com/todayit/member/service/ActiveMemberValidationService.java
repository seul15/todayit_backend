package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 회원이 존재하고 활성 상태인지 검증하는 서비스입니다. */
@Service
public class ActiveMemberValidationService {
  private final MemberRepository memberRepository;

  /**
   * 회원 조회에 필요한 Repository를 받습니다.
   *
   * @param memberRepository 회원 Repository
   */
  public ActiveMemberValidationService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  /**
   * 회원의 존재 여부와 활성 상태를 확인합니다.
   *
   * @param memberId 검증할 회원 식별자
   * @throws BusinessException 회원이 없거나 비활성 상태인 경우
   */
  @Transactional(readOnly = true)
  public void validateActiveMember(String memberId) {
    memberRepository
        .findById(memberId)
        .filter(Member::isActive)
        .orElseThrow(() -> new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));
  }
}
