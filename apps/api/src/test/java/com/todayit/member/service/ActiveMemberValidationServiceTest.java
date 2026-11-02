package com.todayit.member.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.member.entity.Member;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActiveMemberValidationServiceTest {
  @Mock private MemberRepository memberRepository;

  @Mock private Member member;

  private ActiveMemberValidationService service;

  @BeforeEach
  void setUp() {
    service = new ActiveMemberValidationService(memberRepository);
  }

  @Test
  @DisplayName("활성 회원이면 검증을 정상적으로 통과한다")
  void validatesActiveMember() {
    // Given
    String memberId = "member-1";

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(member.isActive()).thenReturn(true);

    // When & Then
    assertDoesNotThrow(() -> service.validateActiveMember(memberId));
  }

  @Test
  @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
  void rejectsNonexistentMember() {
    // Given
    String memberId = "unknown-member";

    when(memberRepository.findById(memberId)).thenReturn(Optional.empty());

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.validateActiveMember(memberId));

    // Then
    assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
  }

  @Test
  @DisplayName("비활성 회원이면 MEMBER_NOT_FOUND 예외가 발생한다")
  void rejectsInactiveMember() {
    // Given
    String memberId = "member-1";

    when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
    when(member.isActive()).thenReturn(false);

    // When
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.validateActiveMember(memberId));

    // Then
    assertEquals(MemberErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
  }
}
