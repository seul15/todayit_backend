package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.member.client.external.GoogleIdentity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GoogleSignupTokenServiceTest {

  @Mock private StringRedisTemplate redisTemplate;

  @Mock private ValueOperations<String, String> valueOperations;

  private GoogleSignupTokenService service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new GoogleSignupTokenService(redisTemplate, objectMapper);
  }

  @Test
  @DisplayName("Google 회원가입 토큰을 해시 Key로 30분간 저장한다")
  void issuesSignupTokenWithThirtyMinuteTtl() throws Exception {
    // Given
    GoogleIdentity identity = new GoogleIdentity("google-user-123", "test@gmail.com", true);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    String token = service.issue(identity);

    // Then
    assertThat(token).matches("[A-Za-z0-9_-]{43}");

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

    verify(valueOperations)
        .set(keyCaptor.capture(), valueCaptor.capture(), eq(Duration.ofMinutes(30)));

    String expectedHash =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));

    assertThat(keyCaptor.getValue()).isEqualTo("auth:oauth-signup:google:" + expectedHash);

    GoogleIdentity storedIdentity =
        objectMapper.readValue(valueCaptor.getValue(), GoogleIdentity.class);

    assertThat(storedIdentity).isEqualTo(identity);
  }

  @Test
  @DisplayName("회원가입 토큰을 발급할 때마다 새로운 값을 생성한다")
  void issuesDifferentTokens() {
    // Given
    GoogleIdentity identity = new GoogleIdentity("google-user-123", "test@gmail.com", true);

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    String firstToken = service.issue(identity);
    String secondToken = service.issue(identity);

    // Then
    assertThat(firstToken).isNotEqualTo(secondToken);

    verify(valueOperations, times(2))
        .set(
            ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), eq(Duration.ofMinutes(30)));
  }

  @Test
  @DisplayName("이메일이 인증되지 않은 Google 정보는 저장하지 않는다")
  void rejectsUnverifiedEmail() {
    // Given
    GoogleIdentity identity = new GoogleIdentity("google-user-123", "test@gmail.com", false);

    // When & Then
    assertThatThrownBy(() -> service.issue(identity)).isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(redisTemplate);
  }

  @Test
  @DisplayName("Google 사용자 고유 식별자가 없으면 토큰을 발급하지 않는다")
  void rejectsMissingProviderUserId() {
    // Given
    GoogleIdentity identity = new GoogleIdentity("", "test@gmail.com", true);

    // When & Then
    assertThatThrownBy(() -> service.issue(identity)).isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(redisTemplate);
  }
}
