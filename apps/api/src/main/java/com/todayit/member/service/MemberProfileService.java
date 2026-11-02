package com.todayit.member.service;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.model.MemberProfileResult;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 회원 프로필 이미지 변경을 처리하는 Command Service입니다. */
@Service
public class MemberProfileService {
  private final MemberRepository memberRepository;

  /**
   * 회원 프로필 변경에 필요한 Repository를 받습니다.
   *
   * @param memberRepository 회원 Repository
   */
  public MemberProfileService(MemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  /**
   * 회원의 프로필 이미지 URL과 수정 시각을 변경합니다.
   *
   * <p>파일 메타데이터 등록과 동일한 DB 트랜잭션에서 호출해야 합니다. 이 메서드에서는 외부 저장소를 호출하지 않습니다.
   *
   * @param memberId 변경할 회원 식별자
   * @param profileImage 업로드가 완료된 프로필 이미지 URL
   * @return 변경된 회원 프로필 정보
   * @throws BusinessException 회원이 존재하지 않거나 비활성 상태인 경우
   */
  @Transactional(propagation = Propagation.MANDATORY)
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
}
