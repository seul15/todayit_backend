package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.member.entity.Member;
import com.todayit.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SignupServiceTest {

  @Mock private MemberRepository memberRepository;

  @Mock private PasswordEncoder passwordEncoder;

  private SignupService signupService;

  @BeforeEach
  void setUp() {
    signupService = new SignupService(memberRepository, passwordEncoder);
  }

  @Test
  @DisplayName("로컬 회원가입 시 비밀번호를 암호화하여 회원을 저장한다")
  void createsLocalMemberWithEncodedPassword() {
    // Given
    String email = "test@test.com";
    String password = "password123!";
    String encodedPassword = "encoded-password";
    String nickname = "테스트";

    when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
    when(memberRepository.save(any(Member.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Member savedMember = signupService.createLocalMember(email, password, nickname);

    // Then
    ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
    verify(memberRepository).save(memberCaptor.capture());

    Member member = memberCaptor.getValue();

    assertThat(member.getPassword()).isEqualTo(encodedPassword);
    assertThat(savedMember).isSameAs(member);
  }
}
