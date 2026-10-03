package com.todayit.member.client.external;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/** Resend REST API를 사용해 이메일을 발송합니다. */
@Component
public class ResendEmailClient {

  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final String EMAIL_PATH = "/emails";

  private final WebClient webClient;
  private final String from;

  /**
   * Resend 이메일 발송 Client를 생성합니다.
   *
   * @param apiKey Resend API Key
   * @param from 발신 이메일 주소
   */
  public ResendEmailClient(
      @Value("${todayit.email.resend.api-key:}") String apiKey,
      @Value("${todayit.email.resend.from:}") String from) {
    this.webClient =
        WebClient.builder()
            .baseUrl("https://api.resend.com")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .build();
    this.from = from;
  }

  /**
   * 이메일 인증번호를 발송합니다.
   *
   * @param email 수신 이메일
   * @param verificationCode 이메일 인증번호
   * @throws IllegalStateException Resend 발신 이메일이 설정되지 않은 경우
   */
  public void sendVerificationCode(String email, String verificationCode) {
    if (from == null || from.isBlank()) {
      throw new IllegalStateException("Resend 발신 이메일이 설정되지 않았습니다.");
    }

    ResendEmailRequest request =
        new ResendEmailRequest(
            from,
            List.of(email),
            "[Today-it] 이메일 인증번호 안내",
            """
                        <div>
                          <h2>Today-it 이메일 인증</h2>
                          <p>아래 인증번호를 입력해 주세요.</p>
                          <p style="font-size: 28px; font-weight: bold;">%s</p>
                          <p>인증번호는 30분 동안 유효합니다.</p>
                        </div>
                        """
                .formatted(verificationCode));

    webClient
        .post()
        .uri(EMAIL_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .block(REQUEST_TIMEOUT);
  }

  private record ResendEmailRequest(String from, List<String> to, String subject, String html) {}
}
