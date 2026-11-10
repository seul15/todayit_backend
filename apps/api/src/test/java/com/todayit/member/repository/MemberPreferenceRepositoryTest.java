package com.todayit.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayit.member.entity.Member;
import com.todayit.theme.Theme;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 회원 선호 지역과 테마의 PostgreSQL 배열 저장 및 조회를 검증합니다. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@Transactional
class MemberPreferenceRepositoryTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRESQL =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("todayit")
          .withUsername("todayit_user")
          .withPassword("todayit1234")
          .withUrlParam("currentSchema", "todayit");

  @Autowired private MemberRepository memberRepository;

  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("기존 회원의 선호 지역과 테마를 수정한 뒤 DB에서 다시 조회한다")
  void persistsUpdatedPreferences() {
    // Given
    Member member =
        Member.createLocal("preference-update@example.com", "encoded-password", "선호수정검증");

    memberRepository.saveAndFlush(member);
    entityManager.clear();

    // When
    Member stored = memberRepository.findById(member.getId()).orElseThrow();

    LocalDateTime now = LocalDateTime.now();

    stored.changePreferredRegions(List.of("11-110", "41-135"), now);
    stored.changePreferredThemes(List.of(Theme.QUIET, Theme.ROMANTIC), now);

    memberRepository.saveAndFlush(stored);
    entityManager.clear();

    // Then
    Member loaded = memberRepository.findById(member.getId()).orElseThrow();

    assertThat(loaded.getPreferredRegions()).containsExactly("11-110", "41-135");
    assertThat(loaded.getPreferredThemes()).containsExactly(Theme.QUIET, Theme.ROMANTIC);
    assertThat(loaded.getNickname()).isEqualTo("선호수정검증");
  }
}
