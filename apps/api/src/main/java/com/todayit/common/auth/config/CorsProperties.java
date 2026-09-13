package com.todayit.common.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS Origin 설정 관리
 *
 * @param origins cross-origin 요청을 허용할 origin 목록
 */
@ConfigurationProperties(prefix = "todayit.security.cors")
public record CorsProperties(List<String> origins) {

  /** 설정 값이 없을때 모든 origin 차단 */
  public CorsProperties {
    origins = origins == null ? List.of() : List.copyOf(origins);
  }
}
