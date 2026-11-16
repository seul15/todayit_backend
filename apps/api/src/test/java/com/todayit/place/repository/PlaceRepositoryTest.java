package com.todayit.place.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayit.course.repository.RegionRepository;
import com.todayit.place.entity.Category;
import com.todayit.place.entity.Place;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL 장소 목록 조회 Repository의 조건과 페이징을 검증합니다. */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
class PlaceRepositoryTest {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceRepositoryTest.class);

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOG.info("테스트 시작: {}", testInfo.getDisplayName());
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    LOG.info("테스트 종료: {}", testInfo.getDisplayName());
  }

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRESQL =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("todayit")
          .withUsername("todayit_user")
          .withPassword("todayit1234")
          .withUrlParam("currentSchema", "todayit");

  @Autowired private PlaceRepository placeRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private RegionRepository regionRepository;

  @Test
  @DisplayName("시도 코드만으로 조회한다")
  @Transactional
  void findsRegionsBySidoOnly() {
    // Given
    jdbcTemplate.update(
        "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES ('41', '111', '경기도', '수원시 장안구'), ('41', '113', '경기도', '수원시 권선구'), ('30', '110', '대전광역시', '동구')");

    // When
    List<Integer> regionIds = regionRepository.findIdsByCodes("41", null);

    // Then
    assertThat(regionIds).hasSize(2);
  }

  @Test
  @DisplayName("시군구 코드만으로 조회한다")
  @Transactional
  void findsRegionsBySigunguOnly() {
    // Given
    jdbcTemplate.update(
        "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES ('41', '111', '경기도', '수원시 장안구'), ('41', '113', '경기도', '수원시 권선구'), ('30', '110', '대전광역시', '동구')");

    // When
    List<Integer> regionIds = regionRepository.findIdsByCodes(null, "111");

    // Then
    assertThat(regionIds).hasSize(1);
  }

  @Test
  @DisplayName("지역 코드 두 조건을 모두 적용한다")
  @Transactional
  void findsRegionsByAllCodes() {
    // Given
    jdbcTemplate.update(
        "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES ('41', '111', '경기도', '수원시 장안구'), ('41', '113', '경기도', '수원시 권선구'), ('30', '110', '대전광역시', '동구')");

    // When
    List<Integer> regionIds = regionRepository.findIdsByCodes("41", "111");

    // Then
    assertThat(regionIds).hasSize(1);
  }

  @Test
  @DisplayName("일치하지 않는 코드 조합은 조회되지 않는다")
  @Transactional
  void findsRegionsByMismatchedCodes() {
    // Given
    jdbcTemplate.update(
        "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES ('41', '111', '경기도', '수원시 장안구'), ('41', '113', '경기도', '수원시 권선구'), ('30', '110', '대전광역시', '동구')");

    // When
    List<Integer> regionIds = regionRepository.findIdsByCodes("11", "111");

    // Then
    assertThat(regionIds).hasSize(0);
  }

  @Test
  @DisplayName("지역과 장소 시드를 재실행해도 코드 기준으로 중복 없이 저장한다")
  @Transactional
  void repeatsCodeSeedsWithoutDuplicates() {
    // When
    jdbcTemplate.execute(
        (ConnectionCallback<Void>)
            connection -> {
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("db/local/R__01_seed_dummy_regions.sql"));
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("db/local/R__04_seed_dummy_places.sql"));
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("db/local/R__01_seed_dummy_regions.sql"));
              ScriptUtils.executeSqlScript(
                  connection, new ClassPathResource("db/local/R__04_seed_dummy_places.sql"));
              return null;
            });

    // Then
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM region WHERE sido_code ~ '^[0-9]{2}$' AND sigungu_code ~ '^[0-9]{3}$'",
                Integer.class))
        .isEqualTo(269);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM place WHERE name LIKE '% 대표 장소'", Integer.class))
        .isEqualTo(269);
    assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM region_place", Integer.class))
        .isEqualTo(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM place", Integer.class));
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT address FROM place WHERE name = '경기도 수원시 장안구 대표 장소'", String.class))
        .isEqualTo("경기도 수원시 장안구 중앙로 1");
  }

  @BeforeEach
  void setUp() {
    jdbcTemplate.update("DELETE FROM place_member_like");
    jdbcTemplate.update("DELETE FROM place_scrap");
    jdbcTemplate.update("DELETE FROM region_place");
    jdbcTemplate.update("DELETE FROM hours");
    jdbcTemplate.update("DELETE FROM place_image");
    jdbcTemplate.update("DELETE FROM place");
  }

  @Test
  @DisplayName("활성화되고 삭제되지 않은 장소만 페이지 단위로 조회한다")
  @Transactional
  void findsActiveAndUndeletedPlaces() {
    // Given
    jdbcTemplate.update(
        """
        INSERT INTO place (
            name, latitude, longitude, address, category, view_count, is_active, is_deleted
        ) VALUES
            ('활성 장소', 37.50000000, 127.00000000, '주소 1', 'RESTAURANT', 10, TRUE, FALSE),
            ('비활성 장소', 37.50000000, 127.00000000, '주소 2', 'CAFE_DESSERT', 20, FALSE, FALSE),
            ('삭제 장소', 37.50000000, 127.00000000, '주소 3', 'RESTAURANT', 30, TRUE, TRUE)
        """);

    // When
    Page<Place> result =
        placeRepository.findPlaces(
            List.of(0), false, List.of(Category.values()), false, PageRequest.of(0, 10));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().getSnapshot().name()).isEqualTo("활성 장소");
  }

  @Test
  @DisplayName("지역과 다중 카테고리 조건이 일치하는 장소를 영업시간 등록 여부와 관계없이 조회한다")
  @Transactional
  void findsPlacesMatchingAllFilters() {
    // Given
    Integer regionId =
        jdbcTemplate.queryForObject(
            "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES ('11', '110', '서울특별시', '종로구') RETURNING region_id",
            Integer.class);
    Integer restaurantId = insertPlace("식당", "RESTAURANT");
    Integer cafeId = insertPlace("카페", "CAFE_DESSERT");
    Integer performanceId = insertPlace("공연장", "PERFORMANCE");
    linkRegion(regionId, restaurantId);
    linkRegion(regionId, cafeId);
    linkRegion(regionId, performanceId);

    // When
    Page<Place> result =
        placeRepository.findPlaces(
            List.of(regionId),
            true,
            List.of(Category.RESTAURANT, Category.CAFE_DESSERT),
            true,
            PageRequest.of(0, 10));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactlyInAnyOrder("식당", "카페");
  }

  @Test
  @DisplayName("상위 지역은 모든 하위 지역을 포함하고 전체 경로는 선택한 하위 지역만 조회한다")
  @Transactional
  void findsPlacesInAllDistrictsOfSelectedCity() {
    // Given
    Integer jongnoRegionId = insertRegion("11", "110", "서울특별시", "종로구");
    Integer mapoRegionId = insertRegion("11", "440", "서울특별시", "마포구");
    Integer suwonRegionId = insertRegion("41", "115", "경기도", "수원시 팔달구");
    Integer jongnoPlaceId = insertPlace("종로 장소", "RESTAURANT");
    Integer mapoPlaceId = insertPlace("마포 장소", "CAFE_DESSERT");
    Integer suwonPlaceId = insertPlace("수원 장소", "RESTAURANT");
    linkRegion(jongnoRegionId, jongnoPlaceId);
    linkRegion(mapoRegionId, mapoPlaceId);
    linkRegion(suwonRegionId, suwonPlaceId);

    // When
    Page<Place> result =
        placeRepository.findPlaces(
            List.of(jongnoRegionId, mapoRegionId),
            true,
            List.of(Category.values()),
            false,
            PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactlyInAnyOrder("종로 장소", "마포 장소");

    Page<Place> districtResult =
        placeRepository.findPlaces(
            List.of(jongnoRegionId),
            true,
            List.of(Category.values()),
            false,
            PageRequest.of(0, 10));
    assertThat(districtResult.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("종로 장소");
  }

  @Test
  @Transactional
  @DisplayName("이름의 일치 글자 수로 정렬하고 페이지를 나누며 비활성 장소를 제외한다")
  void searchesNamesByMatchingCharacterCountAndPaginates() {
    // Given
    insertPlace("gangnam", "RESTAURANT");
    insertPlace("gang", "RESTAURANT");
    insertPlace("xyz", "RESTAURANT");
    Integer inactive = insertPlace("gangnam inactive", "RESTAURANT");
    jdbcTemplate.update("UPDATE place SET is_active = false WHERE place_id = ?", inactive);

    // When
    Page<Place> first = placeRepository.searchByName("gangnam", PageRequest.of(0, 1));
    Page<Place> second = placeRepository.searchByName("gangnam", PageRequest.of(1, 1));

    // Then
    assertThat(first.getTotalElements()).isEqualTo(2);
    assertThat(first.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("gangnam");
    assertThat(second.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("gang");
  }

  @Test
  @Transactional
  @DisplayName("가게 이름의 한글 한 글자만 일치해도 조회하고 일치 글자 수로 정렬한다")
  void searchesKoreanNamesWithSingleCharacterMatches() {
    // Given
    insertPlace("강남카페", "RESTAURANT");
    insertPlace("강변식당", "RESTAURANT");
    insertPlace("서울카페", "RESTAURANT");

    // When
    Page<Place> result = placeRepository.searchByName("강남", PageRequest.of(0, 10));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("강남카페", "강변식당");
  }

  @Test
  @Transactional
  @DisplayName("공백을 제거한 완전 일치가 글자 순서만 다른 이름보다 먼저 조회된다")
  void prioritizesExactNameIgnoringWhitespace() {
    // Given
    insertPlace("성수 어니언", "RESTAURANT");
    insertPlace("어니언 성수", "RESTAURANT");

    // When
    Page<Place> result = placeRepository.searchByName("성수  어니언", PageRequest.of(0, 1));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("성수 어니언");
  }

  @Test
  @Transactional
  @DisplayName("반복 글자는 검색어와 이름의 등장 횟수 중 작은 값만큼 점수에 반영한다")
  void countsRepeatedCharactersUpToKeywordFrequency() {
    // Given
    insertPlace("어어", "RESTAURANT");
    Integer repeatedPlaceId = insertPlace("어어어", "RESTAURANT");
    Integer cafePlaceId = insertPlace("어어 카페", "RESTAURANT");
    insertPlace("어", "RESTAURANT");
    jdbcTemplate.update(
        "UPDATE place SET created_at = TIMESTAMP '2026-01-01 12:00:00' WHERE place_id IN (?, ?)",
        repeatedPlaceId,
        cafePlaceId);

    // When
    Page<Place> result = placeRepository.searchByName("어어", PageRequest.of(0, 10));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(4);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("어어", "어어 카페", "어어어", "어");
  }

  @Test
  @Transactional
  @DisplayName("일치 점수가 같으면 장소 ID보다 생성 시각 내림차순을 먼저 적용한다")
  void prioritizesCreatedAtOverPlaceIdForEqualScores() {
    // Given
    Integer newerPlaceId = insertPlace("어어 카페", "RESTAURANT");
    Integer olderPlaceId = insertPlace("어어어", "RESTAURANT");
    jdbcTemplate.update(
        "UPDATE place SET created_at = TIMESTAMP '2026-01-02 12:00:00' WHERE place_id = ?",
        newerPlaceId);
    jdbcTemplate.update(
        "UPDATE place SET created_at = TIMESTAMP '2026-01-01 12:00:00' WHERE place_id = ?",
        olderPlaceId);

    // When
    Page<Place> result = placeRepository.searchByName("어어", PageRequest.of(0, 10));

    // Then
    assertThat(newerPlaceId).isLessThan(olderPlaceId);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("어어 카페", "어어어");
  }

  @ParameterizedTest
  @CsvSource({
    "어니언성수, 어니언 성수",
    "어니언  성수, 어니언 성수",
    "성수어니언, 성수 어니언",
    "블루엘리펀트, 블루엘리펀트 스페이스 성수",
    "서울숲, 서울숲"
  })
  @Transactional
  @DisplayName("화면의 가게 이름과 공백 변형 검색어로 원하는 장소가 먼저 조회된다")
  void searchesScreenPlaceNames(String keyword, String expectedFirst) {
    // Given
    insertPlace("어니언 성수", "CAFE_DESSERT");
    insertPlace("성수 어니언", "CAFE_DESSERT");
    insertPlace("블루엘리펀트 스페이스 성수", "RESTAURANT");
    insertPlace("서울숲", "RESTAURANT");

    // When
    Page<Place> result = placeRepository.searchByName(keyword, PageRequest.of(0, 10));

    // Then
    assertThat(result.getContent()).isNotEmpty();
    assertThat(result.getContent().getFirst().getSnapshot().name()).isEqualTo(expectedFirst);
  }

  @ParameterizedTest
  @CsvSource({"ZZZZ", "강남", "'   '", "'%'", "'_'"})
  @Transactional
  @DisplayName("일치하지 않는 검색어와 공백 및 와일드카드 문자는 결과를 반환하지 않는다")
  void returnsNoMatchesForUnmatchedKeywords(String keyword) {
    // Given
    insertPlace("어니언 성수", "CAFE_DESSERT");

    // When
    Page<Place> result = placeRepository.searchByName(keyword, PageRequest.of(0, 1));

    // Then
    assertThat(result.getContent()).isEmpty();
    assertThat(result.getTotalElements()).isZero();
  }

  @Test
  @Transactional
  @DisplayName("영문 대소문자를 무시하고 삭제된 장소와 이름 외 필드의 일치를 제외한다")
  void ignoresCaseAndExcludesDeletedPlacesAndAddressMatches() {
    // Given
    insertPlace("ONION", "CAFE_DESSERT");
    Integer deleted = insertPlace("onion deleted", "CAFE_DESSERT");
    jdbcTemplate.update("UPDATE place SET is_deleted = true WHERE place_id = ?", deleted);
    Integer addressMatch = insertPlace("xyz", "CAFE_DESSERT");
    jdbcTemplate.update("UPDATE place SET address = 'onion' WHERE place_id = ?", addressMatch);

    // When
    Page<Place> result = placeRepository.searchByName("onion", PageRequest.of(0, 1));

    // Then
    assertThat(result.getTotalElements()).isEqualTo(1);
    assertThat(result.getContent())
        .extracting(place -> place.getSnapshot().name())
        .containsExactly("ONION");
  }

  private Integer insertRegion(
      String sidoCode, String sigunguCode, String sidoName, String sigunguName) {
    return jdbcTemplate.queryForObject(
        "INSERT INTO region (sido_code, sigungu_code, sido_name, sigungu_name) VALUES (?, ?, ?, ?) RETURNING region_id",
        Integer.class,
        sidoCode,
        sigunguCode,
        sidoName,
        sigunguName);
  }

  private Integer insertPlace(String name, String category) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO place (
            name, latitude, longitude, address, category, view_count, is_active, is_deleted
        ) VALUES (?, 37.50000000, 127.00000000, '종로구 주소', ?, 0, TRUE, FALSE)
        RETURNING place_id
        """,
        Integer.class,
        name,
        category);
  }

  private void linkRegion(int regionId, int placeId) {
    jdbcTemplate.update(
        "INSERT INTO region_place (region_id, place_id) VALUES (?, ?)", regionId, placeId);
  }
}
