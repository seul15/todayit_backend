package com.todayit.common.file.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 환경별 Cloudinary 접속 설정입니다.
 *
 * @param cloudName 제품 환경 이름
 * @param apiKey API 키
 * @param apiSecret API 비밀키
 * @param timeout 호출 제한 시간
 */
@ConfigurationProperties("todayit.file.cloudinary")
public record CloudinaryProperties(
    String cloudName, String apiKey, String apiSecret, Duration timeout) {
  /**
   * 인증 정보가 로그에 노출되지 않도록 설정값을 숨긴 문자열을 반환합니다.
   *
   * @return 인증 정보를 숨긴 설정 설명
   */
  @Override
  public String toString() {
    return "CloudinaryProperties[credentials=REDACTED]";
  }
}
