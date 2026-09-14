package com.todayit.common.auth.jwt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 생성에 필요한 설정값입니다.
 *
 * @param secret JWT 서명에 사용할 비밀키
 * @param accessTokenExpiration Access Token 만료 시간
 */
@ConfigurationProperties(prefix = "todayit.security.jwt")
public record JwtProperties(String secret, Duration accessTokenExpiration) {}
