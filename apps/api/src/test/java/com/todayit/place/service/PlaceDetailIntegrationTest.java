package com.todayit.place.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.todayit.place.dto.response.PlaceDetailResponse;
import com.todayit.place.entity.VisitorInfoType;
import com.todayit.place.service.model.PlaceDetailWithStayResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** 상세 조회의 PostgreSQL 저장 계약을 검증합니다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlaceDetailIntegrationTest {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceDetailIntegrationTest.class);

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOG.info("테스트 시작: {}", testInfo.getDisplayName());
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    LOG.info("테스트 종료: {}", testInfo.getDisplayName());
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired PlaceDetailFacade facade;

  @Test
  @DisplayName("상세 응답은 enum에 해당하고 설명이 있는 안내만 코드와 설명으로 반환한다")
  void returnsOnlyKnownVisitorInfoWithDescriptions() {
    // Given
    int placeId = createPlace();
    jdbc.update(
        "UPDATE place SET visitor_info = CAST(? AS jsonb) WHERE place_id = ?",
        """
        {"PARKING":"지하 주차장 50대 수용 가능","PET_ACCESS":"반려동물 동반 입장 불가",
         "RESTROOM":"   ","TAKEOUT":"","RESERVATION":null,"UNKNOWN":"알 수 없는 항목"}
        """,
        placeId);

    // When
    PlaceDetailResponse response = PlaceDetailResponse.from(facade.getPlaceDetail(placeId, null));

    // Then
    assertThat(response.visitorInfo())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                VisitorInfoType.PARKING, "지하 주차장 50대 수용 가능",
                VisitorInfoType.PET_ACCESS, "반려동물 동반 입장 불가"));
  }

  @Test
  @DisplayName("안내 정보가 없는 장소의 상세 응답은 빈 안내 객체를 반환한다")
  void returnsEmptyVisitorInfoWhenMissing() {
    // Given
    int placeId = createPlace();

    // When
    PlaceDetailResponse response = PlaceDetailResponse.from(facade.getPlaceDetail(placeId, null));

    // Then
    assertThat(response.visitorInfo()).isEmpty();
  }

  private int createPlace() {
    return jdbc.queryForObject(
        """
        INSERT INTO place(name, latitude, longitude, address, category, is_active, is_deleted, description)
        VALUES ('상세 테스트 장소', 37.5, 127.0, '서울', 'CAFE_DESSERT', true, false, '장소 설명')
        RETURNING place_id
        """,
        Integer.class);
  }

  private void createCourseWithStayMinutes(int placeId, int... stayMinutes) {
    createCourseWithStayMinutes(placeId, "ACTIVE", "PUBLIC", stayMinutes);
  }

  private void createCourseWithStayMinutes(
      int placeId, String status, String visibility, int... stayMinutes) {
    String memberId = "stay-average-test-member";
    jdbc.update(
        "INSERT INTO member(member_id, provider, nickname) VALUES (?, 'GOOGLE', '평균 체류 테스트 회원') ON CONFLICT DO NOTHING",
        memberId);
    int courseId =
        jdbc.queryForObject(
            """
        INSERT INTO course(member_id,title,transport,start_at,end_at,status,visibility)
        VALUES (?, '평균 체류 테스트 코스', 'WALKING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
        RETURNING course_id
        """,
            Integer.class,
            memberId,
            status,
            visibility);
    for (int order = 0; order < stayMinutes.length; order++) {
      if (stayMinutes[order] == 0) {
        jdbc.update(
            "INSERT INTO course_place(course_id,place_id,\"order\") VALUES (?,?,?)",
            courseId,
            placeId,
            order);
      } else {
        jdbc.update(
            "INSERT INTO course_place(course_id,place_id,\"order\",stay_minute) VALUES (?,?,?,?)",
            courseId,
            placeId,
            order,
            stayMinutes[order]);
      }
    }
  }

  @Test
  @DisplayName("활성 공개·비공개 코스는 평균에 포함하고 임시저장·삭제 코스는 제외한다")
  void averagesOnlyActiveCoursesRegardlessOfVisibility() {
    // Given
    int placeId = createPlace();
    createCourseWithStayMinutes(placeId, "ACTIVE", "PUBLIC", 0, 60);
    createCourseWithStayMinutes(placeId, "ACTIVE", "PRIVATE", 90);
    createCourseWithStayMinutes(placeId, "DRAFT", "PUBLIC", 600);
    createCourseWithStayMinutes(placeId, "DELETED", "PRIVATE", 900);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, null);

    // Then
    assertThat(result.averageStayMinutes()).isEqualTo(75);
  }

  @Test
  @DisplayName("임시저장·삭제 코스만 존재하면 평균 체류 시간은 0분이다")
  void returnsZeroWhenOnlyDraftAndDeletedCoursesExist() {
    // Given
    int placeId = createPlace();
    createCourseWithStayMinutes(placeId, "DRAFT", "PUBLIC", 60);
    createCourseWithStayMinutes(placeId, "DELETED", "PRIVATE", 90);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, null);

    // Then
    assertThat(result.averageStayMinutes()).isZero();
  }

  @Test
  @DisplayName("미입력 기본값 0분은 평균에서 제외하고 입력된 체류 시간만 평균 낸다")
  void excludesDefaultZeroStayMinutesFromAverage() {
    // Given
    int placeId = createPlace();
    createCourseWithStayMinutes(placeId, 0, 60, 90);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, null);

    // Then
    assertThat(result.averageStayMinutes()).isEqualTo(75);
  }

  @Test
  @DisplayName("체류 시간이 모두 미입력 기본값 0분이면 평균 체류 시간은 0분이다")
  void returnsZeroWhenAllStayMinutesAreDefaultZero() {
    // Given
    int placeId = createPlace();
    createCourseWithStayMinutes(placeId, 0, 0);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, null);

    // Then
    assertThat(result.averageStayMinutes()).isZero();
  }

  @Test
  @DisplayName("관련 데이터가 없는 장소는 평균 0분과 빈 영업시간 및 이미지 목록을 반환한다")
  void returnsZeroAndEmptyListsWhenNoRelatedRecordsExist() {
    // Given
    int placeId = createPlace();

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, null);

    // Then
    assertThat(result.averageStayMinutes()).isZero();
    assertThat(result.detail().description()).isEqualTo("장소 설명");
    assertThat(result.detail().openingHours()).isEmpty();
    assertThat(result.detail().place().imageUrls()).isEmpty();
    assertThat(result.detail().liked()).isFalse();
    assertThat(result.detail().scrapped()).isFalse();
  }

  @Test
  @DisplayName("활성 코스에서 해당 장소의 체류 시간만 평균 내고 회원 상태를 반환한다")
  void averagesOnlyMatchingPlaceAcrossActiveCoursesAndReturnsMemberStates() {
    // Given
    int placeId = createPlace();
    int otherPlaceId = createPlace();
    String memberId = "detail-test-member";
    jdbc.update(
        "INSERT INTO member(member_id, provider, nickname) VALUES (?, 'GOOGLE', '상세 테스트 회원')",
        memberId);
    int order = 0;
    for (String status : List.of("DRAFT", "ACTIVE", "DELETED")) {
      int courseId =
          jdbc.queryForObject(
              """
          INSERT INTO course(member_id,title,transport,start_at,end_at,status,visibility)
          VALUES (?, '테스트 코스', 'WALKING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, 'PRIVATE')
          RETURNING course_id
          """,
              Integer.class,
              memberId,
              status);
      jdbc.update(
          "INSERT INTO course_place(course_id, place_id, stay_minute) VALUES (?, ?, ?)",
          courseId,
          placeId,
          30 + order++);
      jdbc.update(
          "INSERT INTO course_place(course_id, place_id, stay_minute, \"order\") VALUES (?, ?, 999, 1)",
          courseId,
          otherPlaceId);
    }
    jdbc.update(
        "INSERT INTO place_member_like(member_id,place_id) VALUES (?,?)", memberId, placeId);
    jdbc.update(
        "INSERT INTO place_scrap(member_id,place_id,is_deleted) VALUES (?,?,false)",
        memberId,
        placeId);
    jdbc.update(
        "INSERT INTO place_image(place_id,image_url) VALUES (?,?)",
        placeId,
        "https://example.com/place.jpg");
    jdbc.update(
        """
        INSERT INTO hours(place_id,open_date,start_at,end_at)
        VALUES (?, '2026-10-16 00:00:00+09', '2026-10-16 09:00:00+09', '2026-10-16 18:00:00+09')
        """,
        placeId);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, memberId);

    // Then
    assertThat(result.averageStayMinutes()).isEqualTo(31);
    assertThat(result.detail().openingHours()).hasSize(1);
    assertThat(result.detail().place().imageUrls())
        .containsExactly("https://example.com/place.jpg");
    assertThat(result.detail().liked()).isTrue();
    assertThat(result.detail().scrapped()).isTrue();
  }

  @Test
  @DisplayName("취소된 스크랩은 장소 상세에서 false로 반환한다")
  void returnsFalseForCancelledScrap() {
    // Given
    int placeId = createPlace();
    String memberId = "cancelled-scrap-member";
    jdbc.update(
        "INSERT INTO member(member_id, provider, nickname) VALUES (?, 'GOOGLE', '취소 스크랩 테스트 회원')",
        memberId);
    jdbc.update(
        "INSERT INTO place_scrap(member_id,place_id,is_deleted) VALUES (?,?,true)",
        memberId,
        placeId);

    // When
    PlaceDetailWithStayResult result = facade.getPlaceDetail(placeId, memberId);

    // Then
    assertThat(result.detail().scrapped()).isFalse();
  }
}
