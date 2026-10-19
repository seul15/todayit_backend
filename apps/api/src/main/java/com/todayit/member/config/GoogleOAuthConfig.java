package com.todayit.member.config;

import com.todayit.member.client.external.GoogleOAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** API 서버에서 사용하는 Google OAuth 연동 설정입니다. */
@Configuration(proxyBeanMethods = false)
public class GoogleOAuthConfig {

  /**
   * API 서버에서 Google OAuth 클라이언트를 등록합니다.
   *
   * @param clientId Google OAuth Client ID
   * @param clientSecret Google OAuth Client Secret
   * @param redirectUri Google에 등록한 Redirect URI
   * @return Google OAuth 클라이언트
   */
  @Bean
  public GoogleOAuthClient googleOAuthClient(
      @Value("${todayit.oauth.google.client-id:}") String clientId,
      @Value("${todayit.oauth.google.client-secret:}") String clientSecret,
      @Value("${todayit.oauth.google.redirect-uri}") String redirectUri) {

    return new GoogleOAuthClient(WebClient.builder(), clientId, clientSecret, redirectUri);
  }
}
