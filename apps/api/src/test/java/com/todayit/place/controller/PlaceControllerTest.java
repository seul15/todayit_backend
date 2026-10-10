package com.todayit.place.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.exception.GlobalExceptionHandler;
import com.todayit.common.pagination.PageResult;
import com.todayit.course.service.CoursePlaceQueryService;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;
import com.todayit.place.entity.Category;
import com.todayit.place.exception.PlaceNotFoundException;
import com.todayit.place.service.PlaceCommandService;
import com.todayit.place.service.PlaceQueryService;
import com.todayit.place.service.model.PlaceImageResult;
import com.todayit.place.service.model.PlaceLikeResult;
import com.todayit.place.service.model.PlaceLocationResult;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceScrapResult;
import com.todayit.place.service.model.PlaceSort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 장소 목록 조회 Controller의 HTTP 응답을 검증합니다. */
@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

  @Mock private PlaceQueryService placeQueryService;

  @Mock private PlaceCommandService placeCommandService;

  @Mock private CoursePlaceQueryService coursePlaceQueryService;

  private MockMvc mockMvc;

  /** Controller와 공통 예외 처리기를 MockMvc에 등록합니다. */
  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(
                new PlaceController(
                    placeQueryService, placeCommandService, coursePlaceQueryService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  /**
   * 장소 목록과 페이지 메타데이터를 성공 응답으로 반환하는지 검증합니다.
   *
   * @throws Exception MockMvc 요청 처리 중 예외
   */
  @Test
  @DisplayName("장소 목록을 페이지 정보와 함께 반환한다")
  void returnsPagedPlaces() throws Exception {
    // Given
    PlaceResult place =
        new PlaceResult(
            1,
            "오늘의 식당",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "서울특별시 종로구 종로 1",
            Category.RESTAURANT,
            10,
            List.of("https://placehold.co/1200x800?text=Restaurant"));
    when(placeQueryService.findPlaces(1, 5, PlaceSort.POPULAR))
        .thenReturn(new PageResult<>(List.of(place), 1, 5, 6));

    // When
    mockMvc
        .perform(
            get("/api/v1/places").param("page", "1").param("size", "5").param("sort", "POPULAR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].placeId").value(1))
        .andExpect(jsonPath("$.data.content[0].name").value("오늘의 식당"))
        .andExpect(jsonPath("$.data.content[0].address").value("서울특별시 종로구 종로 1"))
        .andExpect(
            jsonPath("$.data.content[0].imageUrls[0]")
                .value("https://placehold.co/1200x800?text=Restaurant"))
        .andExpect(jsonPath("$.data.page").value(1))
        .andExpect(jsonPath("$.data.size").value(5))
        .andExpect(jsonPath("$.data.totalElements").value(6))
        .andExpect(jsonPath("$.data.totalPages").value(2));

    // Then
    verify(placeQueryService).findPlaces(1, 5, PlaceSort.POPULAR);
  }

  /** 특정 장소를 포함한 코스 목록을 페이지 정보와 함께 반환하는지 검증합니다. */
  @Test
  @DisplayName("해당 장소로 만들어진 코스를 페이지 정보와 함께 반환한다")
  void returnsCoursesByPlace() throws Exception {
    // Given
    CourseResult course =
        new CourseResult(
            1,
            "종로 맛집과 카페 코스",
            "종로에서 식사와 카페를 함께 즐기는 코스입니다.",
            com.todayit.course.entity.CourseTransport.WALKING,
            java.time.LocalDateTime.of(2026, 10, 3, 11, 0),
            java.time.LocalDateTime.of(2026, 10, 3, 15, 0),
            25);
    when(coursePlaceQueryService.findCoursesByPlace(1, 0, 20, CourseSort.LATEST))
        .thenReturn(new PageResult<>(List.of(course), 0, 20, 1));

    // When
    mockMvc
        .perform(get("/api/v1/places/1/courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].courseId").value(1))
        .andExpect(jsonPath("$.data.content[0].title").value("종로 맛집과 카페 코스"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.totalPages").value(1));

    // Then
    verify(coursePlaceQueryService).findCoursesByPlace(1, 0, 20, CourseSort.LATEST);
  }

  @Test
  @DisplayName("존재하지 않는 장소의 코스 조회는 장소 없음 오류를 반환한다")
  void returnsNotFoundWhenCoursesPlaceDoesNotExist() throws Exception {
    // Given
    doThrow(new PlaceNotFoundException()).when(placeQueryService).validatePlaceExists(999);

    // When
    mockMvc
        .perform(get("/api/v1/places/999/courses"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));

    // Then
    verifyNoInteractions(coursePlaceQueryService);
  }

  /** 장소 사진 목록과 페이지 정보를 성공 응답으로 반환하는지 검증합니다. */
  @Test
  @DisplayName("장소 사진 목록을 페이지 정보와 함께 반환한다")
  void returnsPagedPlaceImages() throws Exception {
    // Given
    when(placeQueryService.findPlaceImages(1, 0, 5))
        .thenReturn(
            new PageResult<>(
                List.of(new PlaceImageResult(10, "https://example.com/place-image.jpg")), 0, 5, 1));

    // When
    mockMvc
        .perform(get("/api/v1/places/1/images").param("page", "0").param("size", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.content[0].placeImageId").value(10))
        .andExpect(
            jsonPath("$.data.content[0].imageUrl").value("https://example.com/place-image.jpg"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(5))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.totalPages").value(1));

    // Then
    verify(placeQueryService).findPlaceImages(1, 0, 5);
  }

  /** 인증된 회원의 장소 스크랩 결과를 반환하는지 검증합니다. */
  @Test
  @DisplayName("장소를 스크랩하고 스크랩 수를 반환한다")
  void scrapsPlace() throws Exception {
    // Given
    when(placeCommandService.scrapPlace(1, "member-1"))
        .thenReturn(new PlaceScrapResult(1, true, 3));

    // When
    mockMvc
        .perform(
            post("/api/v1/places/1/scrap")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.placeId").value(1))
        .andExpect(jsonPath("$.data.scrapped").value(true))
        .andExpect(jsonPath("$.data.scrapCount").value(3));

    // Then
    verify(placeCommandService).scrapPlace(1, "member-1");
  }

  /** 인증된 회원의 장소 좋아요 결과를 반환하는지 검증합니다. */
  @Test
  @DisplayName("장소를 좋아요하고 좋아요 수를 반환한다")
  void likesPlace() throws Exception {
    // Given
    when(placeCommandService.likePlace(1, "member-1")).thenReturn(new PlaceLikeResult(1, true, 3));

    // When
    mockMvc
        .perform(
            post("/api/v1/places/1/like")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.placeId").value(1))
        .andExpect(jsonPath("$.data.liked").value(true))
        .andExpect(jsonPath("$.data.likeCount").value(3));

    // Then
    verify(placeCommandService).likePlace(1, "member-1");
  }

  /** 인증된 회원의 장소 스크랩 취소 결과를 반환하는지 검증합니다. */
  @Test
  @DisplayName("장소 스크랩을 취소하고 남은 스크랩 수를 반환한다")
  void cancelsPlaceScrap() throws Exception {
    // Given
    when(placeCommandService.cancelPlaceScrap(1, "member-1"))
        .thenReturn(new PlaceScrapResult(1, false, 2));

    // When
    mockMvc
        .perform(
            delete("/api/v1/places/1/scrap")
                .principal(new UsernamePasswordAuthenticationToken("member-1", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.placeId").value(1))
        .andExpect(jsonPath("$.data.scrapped").value(false))
        .andExpect(jsonPath("$.data.scrapCount").value(2));

    // Then
    verify(placeCommandService).cancelPlaceScrap(1, "member-1");
  }

  /**
   * 음수 페이지 번호를 잘못된 요청으로 처리하는지 검증합니다.
   *
   * @throws Exception MockMvc 요청 처리 중 예외
   */
  @Test
  @DisplayName("페이지 번호가 음수이면 400 응답을 반환한다")
  void returnsBadRequestWhenPageIsNegative() throws Exception {
    // Given
    String invalidPage = "-1";

    // When
    mockMvc
        .perform(get("/api/v1/places").param("page", invalidPage))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"));

    // Then
    verifyNoInteractions(placeQueryService, placeCommandService);
  }

  /**
   * 0 이하의 페이지 크기를 장소 전용 커스텀 오류로 처리하는지 검증합니다.
   *
   * @throws Exception MockMvc 요청 처리 중 예외
   */
  @Test
  @DisplayName("페이지 크기가 0 이하이면 장소 전용 커스텀 오류를 반환한다")
  void returnsPlaceCustomErrorWhenSizeIsNotPositive() throws Exception {
    // Given
    String invalidSize = "0";

    // When
    mockMvc
        .perform(get("/api/v1/places").param("size", invalidSize))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("INVALID_PAGINATION"))
        .andExpect(jsonPath("$.message").value("페이지 요청값이 올바르지 않습니다."));

    // Then
    verifyNoInteractions(placeQueryService, placeCommandService);
  }

  /**
   * 장소의 좌표와 주소를 지도 조회 응답으로 반환하는지 검증합니다.
   *
   * @throws Exception MockMvc 요청 처리 중 예외
   */
  @Test
  @DisplayName("장소 지도 정보를 반환한다")
  void returnsPlaceLocation() throws Exception {
    // Given
    when(placeQueryService.findPlaceLocation(1))
        .thenReturn(
            new PlaceLocationResult(
                1,
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1"));

    // When
    mockMvc
        .perform(get("/api/v1/places/1/location"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.placeId").value(1))
        .andExpect(jsonPath("$.data.latitude").value(37.57))
        .andExpect(jsonPath("$.data.longitude").value(126.985))
        .andExpect(jsonPath("$.data.address").value("서울특별시 종로구 종로 1"));

    // Then
    verify(placeQueryService).findPlaceLocation(1);
  }

  /**
   * 존재하지 않는 장소 요청을 장소 전용 404 오류로 반환하는지 검증합니다.
   *
   * @throws Exception MockMvc 요청 처리 중 예외
   */
  @Test
  @DisplayName("존재하지 않는 장소의 지도 조회는 404 응답을 반환한다")
  void returnsNotFoundWhenPlaceLocationDoesNotExist() throws Exception {
    // Given
    when(placeQueryService.findPlaceLocation(999)).thenThrow(new PlaceNotFoundException());

    // When
    mockMvc
        .perform(get("/api/v1/places/999/location"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));

    // Then
    verify(placeQueryService).findPlaceLocation(999);
  }
}
