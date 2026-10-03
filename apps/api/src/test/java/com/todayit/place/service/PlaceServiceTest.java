package com.todayit.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.pagination.PageResult;
import com.todayit.place.entity.Category;
import com.todayit.place.entity.Place;
import com.todayit.place.repository.PlaceImageRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceLocationResult;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceSort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** 장소 목록 조회 Service의 업무 결과를 검증합니다. */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

  @Mock private PlaceRepository placeRepository;

  @Mock private PlaceImageRepository placeImageRepository;

  @Mock private PlaceScrapRepository placeScrapRepository;

  @Mock private Place place;

  /** 장소를 인기순으로 조회하고 서비스 결과로 변환하는지 검증합니다. */
  @Test
  @DisplayName("장소 목록을 인기순으로 조회하고 페이지 정보와 함께 반환한다")
  void findsPlacesByPopularity() {
    // Given
    when(place.getPlaceId()).thenReturn(1);
    Place.PlaceSnapshot snapshot =
        new Place.PlaceSnapshot(
            1,
            "오늘의 식당",
            new BigDecimal("37.5"),
            new BigDecimal("127.0"),
            "서울특별시 종로구 종로 1",
            Category.RESTAURANT,
            15,
            List.of("https://placehold.co/1200x800?text=Restaurant"));
    when(place.getSnapshot(anyList())).thenReturn(snapshot);
    when(placeRepository.findByIsActiveTrueAndIsDeletedFalse(
            argThat(
                pageable ->
                    pageable.equals(
                        PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "viewCount"))))))
        .thenReturn(new PageImpl<>(List.of(place), PageRequest.of(1, 2), 3));
    PlaceService placeService =
        new PlaceService(placeRepository, placeImageRepository, placeScrapRepository);

    // When
    PageResult<PlaceResult> result = placeService.findPlaces(1, 2, PlaceSort.POPULAR);

    // Then
    assertThat(result.content())
        .containsExactly(
            new PlaceResult(
                1,
                "오늘의 식당",
                new BigDecimal("37.5"),
                new BigDecimal("127.0"),
                "서울특별시 종로구 종로 1",
                Category.RESTAURANT,
                15,
                List.of("https://placehold.co/1200x800?text=Restaurant")));
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3);
    verify(placeRepository)
        .findByIsActiveTrueAndIsDeletedFalse(
            PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "viewCount")));
    verify(placeImageRepository).findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(List.of(1));
  }

  /** 장소 Entity에서 지도 조회에 필요한 위치 정보를 반환하는지 검증합니다. */
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
    PlaceService placeService =
        new PlaceService(placeRepository, placeImageRepository, placeScrapRepository);

    // When
    PlaceLocationResult result = placeService.findPlaceLocation(1);

    // Then
    assertThat(result)
        .isEqualTo(
            new PlaceLocationResult(
                1,
                new BigDecimal("37.57000000"),
                new BigDecimal("126.98500000"),
                "서울특별시 종로구 종로 1"));
  }

  /** 존재하지 않는 장소를 조회하면 장소 없음 예외를 발생시키는지 검증합니다. */
  @Test
  @DisplayName("존재하지 않는 장소의 지도 조회는 장소 없음 예외를 발생시킨다")
  void throwsWhenPlaceLocationDoesNotExist() {
    // Given
    when(placeRepository.findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(999))
        .thenReturn(Optional.empty());
    PlaceService placeService =
        new PlaceService(placeRepository, placeImageRepository, placeScrapRepository);

    // When
    var exception = assertThatThrownBy(() -> placeService.findPlaceLocation(999));

    // Then
    exception.isInstanceOf(com.todayit.place.exception.PlaceNotFoundException.class);
  }
}
