package com.todayit.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.service.RegionPlaceQueryService;
import com.todayit.course.service.model.RegionFilter;
import com.todayit.place.entity.Category;
import com.todayit.place.entity.Place;
import com.todayit.place.repository.PlaceImageRepository;
import com.todayit.place.repository.PlaceMemberLikeRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceLocationResult;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceScrapSort;
import com.todayit.place.service.model.PlaceSort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.AbstractThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class PlaceQueryServiceTest {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceQueryServiceTest.class);

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOG.info("테스트 시작: {}", testInfo.getDisplayName());
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    LOG.info("테스트 종료: {}", testInfo.getDisplayName());
  }

  @Mock private PlaceMemberLikeRepository placeMemberLikeRepository;

  @Mock private PlaceRepository placeRepository;

  @Mock private PlaceImageRepository placeImageRepository;

  @Mock private PlaceScrapRepository placeScrapRepository;

  @Mock private RegionPlaceQueryService regionPlaceQueryService;

  @Mock private Place place;

  @Test
  @DisplayName("지역 필터 없이 장소를 조회한다")
  void findsPlacesWithoutRegion() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(placeRepository.findPlaces(
            List.of(0), false, List.of(Category.RESTAURANT, Category.CAFE_DESSERT), true, pageable))
        .thenReturn(Page.empty(pageable));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(
            null,
            null,
            List.of(Category.RESTAURANT, Category.CAFE_DESSERT),
            0,
            20,
            PlaceSort.LATEST,
            null);

    // Then
    assertThat(result.content()).isEmpty();
    verify(placeRepository)
        .findPlaces(
            List.of(0), false, List.of(Category.RESTAURANT, Category.CAFE_DESSERT), true, pageable);
  }

  @Test
  @DisplayName("카테고리 필터 없이 장소를 조회한다")
  void findsPlacesWithoutCategories() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(placeRepository.findPlaces(List.of(0), false, List.of(Category.values()), false, pageable))
        .thenReturn(Page.empty(pageable));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(null, null, null, 0, 20, PlaceSort.LATEST, null);

    // Then
    assertThat(result.content()).isEmpty();
    verify(placeRepository)
        .findPlaces(List.of(0), false, List.of(Category.values()), false, pageable);
  }

  @Test
  @DisplayName("비회원으로 조회하면 좋아요 여부는 false이다")
  void returnsLikedFalseForAnonymousUser() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(place.getPlaceId()).thenReturn(1);
    when(place.getSnapshot(List.of()))
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "오늘의 식당",
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    when(placeRepository.findPlaces(List.of(0), false, List.of(Category.values()), false, pageable))
        .thenReturn(new PageImpl<>(List.of(place), pageable, 1));
    when(placeImageRepository.findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(List.of(1)))
        .thenReturn(List.of());
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(null, null, null, 0, 20, PlaceSort.LATEST, null);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).liked()).isFalse();
    org.mockito.Mockito.verifyNoInteractions(placeMemberLikeRepository, regionPlaceQueryService);
  }

  @Test
  @DisplayName("회원으로 조회하면 좋아요한 장소의 liked는 true이다")
  void returnsLikedTrueForLikedPlace() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(place.getPlaceId()).thenReturn(1);
    when(place.getSnapshot(List.of()))
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "오늘의 식당",
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    when(placeRepository.findPlaces(List.of(0), false, List.of(Category.values()), false, pageable))
        .thenReturn(new PageImpl<>(List.of(place), pageable, 1));
    when(placeImageRepository.findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(List.of(1)))
        .thenReturn(List.of());
    when(placeMemberLikeRepository.findLikedPlaceIds("member-1", List.of(1)))
        .thenReturn(java.util.Set.of(1));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(null, null, null, 0, 20, PlaceSort.LATEST, "member-1");

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).liked()).isTrue();
  }

  @Test
  @DisplayName("조회 결과가 비어있으면 좋아요 저장소를 조회하지 않는다")
  void doesNotQueryLikesWhenPlacesEmpty() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(placeRepository.findPlaces(List.of(0), false, List.of(Category.values()), false, pageable))
        .thenReturn(Page.empty(pageable));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(null, null, null, 0, 20, PlaceSort.LATEST, "member-1");

    // Then
    assertThat(result.content()).isEmpty();
    org.mockito.Mockito.verifyNoInteractions(placeMemberLikeRepository, regionPlaceQueryService);
  }

  @Test
  @DisplayName("지역과 카테고리 필터를 조합해 장소를 조회한다")
  void findsPlacesWithFilters() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
    when(regionPlaceQueryService.findRegionIds(List.of(new RegionFilter("11", List.of("230")))))
        .thenReturn(List.of(10));
    when(placeRepository.findPlaces(
            List.of(10), true, List.of(Category.RESTAURANT, Category.CAFE_DESSERT), true, pageable))
        .thenReturn(Page.empty(pageable));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findPlaces(
            "11",
            "230",
            List.of(Category.RESTAURANT, Category.CAFE_DESSERT),
            0,
            20,
            PlaceSort.LATEST,
            null);

    // Then
    assertThat(result.content()).isEmpty();
    verify(placeRepository)
        .findPlaces(
            List.of(10), true, List.of(Category.RESTAURANT, Category.CAFE_DESSERT), true, pageable);
  }

  @Test
  @DisplayName("장소 식별자로 좌표와 주소를 조회한다")
  void findsPlaceLocation() {
    // Given
    when(place.getSnapshot())
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "오늘의 식당",
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    when(placeRepository.findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(1))
        .thenReturn(Optional.of(place));
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PlaceLocationResult result = placeQueryService.findPlaceLocation(1);

    // Then
    assertThat(result.placeId()).isEqualTo(1);
    assertThat(result.latitude()).isEqualByComparingTo(new BigDecimal("37.57000000"));
    assertThat(result.longitude()).isEqualByComparingTo(new BigDecimal("126.98500000"));
    assertThat(result.address()).isEqualTo("서울특별시 종로구 종로 1");
  }

  @Test
  @DisplayName("존재하지 않는 장소 식별자로 위치를 조회하면 예외를 던진다")
  void throwsExceptionWhenPlaceNotFoundForLocation() {
    // Given
    when(placeRepository.findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(999))
        .thenReturn(Optional.empty());
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When & Then
    AbstractThrowableAssert<?, ? extends Throwable> throwableAssert =
        assertThatThrownBy(() -> placeQueryService.findPlaceLocation(999));
    throwableAssert.isInstanceOf(com.todayit.place.exception.PlaceNotFoundException.class);
  }

  @Test
  @DisplayName("스크랩한 장소를 최신순으로 조회한다")
  void findsScrappedPlacesLatest() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20);
    when(place.getPlaceId()).thenReturn(1);
    when(place.getSnapshot(List.of()))
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "오늘의 식당",
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    when(placeScrapRepository.findScrappedPlacesLatest("member-1", pageable))
        .thenReturn(new PageImpl<>(List.of(place), pageable, 1));
    when(placeImageRepository.findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(List.of(1)))
        .thenReturn(List.of());
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findScrappedPlaces("member-1", 0, 20, PlaceScrapSort.LATEST);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).name()).isEqualTo("오늘의 식당");
  }

  @Test
  @DisplayName("스크랩한 장소를 과거순으로 조회한다")
  void findsScrappedPlacesOldest() {
    // Given
    PageRequest pageable = PageRequest.of(0, 20);
    when(place.getPlaceId()).thenReturn(1);
    when(place.getSnapshot(List.of()))
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "오늘의 식당",
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    when(placeScrapRepository.findScrappedPlacesOldest("member-1", pageable))
        .thenReturn(new PageImpl<>(List.of(place), pageable, 1));
    when(placeImageRepository.findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(List.of(1)))
        .thenReturn(List.of());
    PlaceQueryService placeQueryService =
        new PlaceQueryService(
            placeMemberLikeRepository,
            placeRepository,
            placeImageRepository,
            placeScrapRepository,
            regionPlaceQueryService);

    // When
    PageResult<PlaceResult> result =
        placeQueryService.findScrappedPlaces("member-1", 0, 20, PlaceScrapSort.OLDEST);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().get(0).name()).isEqualTo("오늘의 식당");
  }
}
