package com.todayit.common.auth.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 관리에 필요한 설정값입니다.
 *
 * @param expiration Refresh Token 유효 시간
 */
@ConfigurationProperties(prefix = "todayit.security.refresh-token")
public record RefreshTokenProperties(Duration expiration) {}
