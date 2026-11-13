package com.todayit.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayit.course.entity.Region;
import com.todayit.course.repository.RegionRepository;
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

/** 회원 선호 지역 코드와 테마 배열의 PostgreSQL 저장 및 조회를 검증합니다. */
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
  @Autowired private RegionRepository regionRepository;
  @Autowired private EntityManager entityManager;

  @Test
  @DisplayName("회원 선호 지역 FK와 테마 배열을 저장하고 다시 조회한다")
  void persistsUpdatedPreferences() {
    entityManager
        .createNativeQuery(
            """
                    INSERT INTO region (
                        sido_code, sigungu_code, sido_name, sigungu_name
                    )
                    VALUES ('11', '110', '서울특별시', '종로구')
                    """)
        .executeUpdate();

    Member member =
        Member.createLocal("preference-update@example.com", "encoded-password", "선호수정검증");

    memberRepository.saveAndFlush(member);
    entityManager.clear();

    Integer regionId = regionRepository.findIdsByCodes("11", "110").getFirst();

    Region region = regionRepository.findById(regionId).orElseThrow();

    Member stored = memberRepository.findById(member.getId()).orElseThrow();

    LocalDateTime now = LocalDateTime.now();

    stored.changePreferredRegion(region, now);
    stored.changePreferredThemes(List.of(Theme.QUIET, Theme.ROMANTIC), now);

    memberRepository.saveAndFlush(stored);
    entityManager.clear();

    // Then
    Member loaded = memberRepository.findById(member.getId()).orElseThrow();

    assertThat(loaded.getPreferredRegion()).isNotNull();
    assertThat(loaded.getPreferredRegion().getRegionId()).isEqualTo(regionId);
    assertThat(loaded.getPreferredRegion().getSidoCode()).isEqualTo("11");
    assertThat(loaded.getPreferredRegion().getSigunguCode()).isEqualTo("110");

    assertThat(loaded.getPreferredThemes()).containsExactly(Theme.QUIET, Theme.ROMANTIC);

    assertThat(loaded.getNickname()).isEqualTo("선호수정검증");
  }
}
