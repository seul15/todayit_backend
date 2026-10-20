package com.todayit.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.place.entity.Category;
import com.todayit.place.entity.Hours;
import com.todayit.place.entity.Place;
import com.todayit.place.exception.PlaceNotFoundException;
import com.todayit.place.repository.PlaceMemberLikeRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceDetailResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

@ExtendWith(MockitoExtension.class)
class PlaceDetailQueryServiceTest {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceDetailQueryServiceTest.class);

  @BeforeEach
  void logTestStart(TestInfo testInfo) {
    LOG.info("테스트 시작: {}", testInfo.getDisplayName());
  }

  @AfterEach
  void logTestEnd(TestInfo testInfo) {
    LOG.info("테스트 종료: {}", testInfo.getDisplayName());
  }

  @Mock PlaceRepository places;
  @Mock PlaceMemberLikeRepository likes;
  @Mock PlaceScrapRepository scraps;
  @Mock Place place;

  private PlaceDetailQueryService service() {
    return new PlaceDetailQueryService(places, likes, scraps);
  }

  private void existingPlace() {
    when(places.findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(1)).thenReturn(Optional.of(place));
    when(place.getSnapshot())
        .thenReturn(
            new Place.PlaceSnapshot(
                1,
                "카페",
                BigDecimal.ONE,
                BigDecimal.TEN,
                "서울",
                Category.CAFE_DESSERT,
                0,
                List.of()));
  }

  @Test
  @DisplayName("비회원은 좋아요와 스크랩이 false이며 영업시간이 없으면 빈 목록을 반환한다")
  void anonymousUserHasNoLikesOrScrapsAndEmptyHours() {
    // Given
    existingPlace();

    // When
    PlaceDetailResult result = service().getPlaceDetail(1, null);

    // Then
    assertThat(result.liked()).isFalse();
    assertThat(result.scrapped()).isFalse();
    assertThat(result.openingHours()).isEmpty();
    assertThat(result.description()).isNull();
    verifyNoInteractions(likes, scraps);
  }

  @Test
  @DisplayName("회원의 좋아요와 스크랩 여부 및 날짜순 영업시간을 반환한다")
  void returnsMemberStatesAndChronologicalHours() {
    // Given
    existingPlace();
    when(likes.existsByMemberIdAndPlacePlaceId("member-1", 1)).thenReturn(true);
    when(scraps.existsByMemberIdAndPlacePlaceIdAndIsDeletedFalse("member-1", 1)).thenReturn(true);
    Hours later = mock(Hours.class);
    Hours earlier = mock(Hours.class);
    LocalDateTime day = LocalDateTime.of(2026, 10, 16, 0, 0);
    when(later.getOpenDate()).thenReturn(day.plusDays(1));
    when(earlier.getOpenDate()).thenReturn(day);
    when(place.getHours()).thenReturn(List.of(later, earlier));

    // When
    PlaceDetailResult result = service().getPlaceDetail(1, "member-1");

    // Then
    assertThat(result.liked()).isTrue();
    assertThat(result.scrapped()).isTrue();
    assertThat(result.openingHours())
        .extracting(h -> h.openDate())
        .containsExactly(day, day.plusDays(1));
  }

  @Test
  @DisplayName("조회 불가능한 장소는 회원 상태 조회 전에 예외를 발생시킨다")
  void missingOrUnavailablePlaceFailsBeforeMemberLookup() {
    // Given
    PlaceDetailQueryService queryService = service();

    // When
    Throwable exception = catchThrowable(() -> queryService.getPlaceDetail(1, "member-1"));

    // Then
    assertThat(exception).isInstanceOf(PlaceNotFoundException.class);
    verifyNoInteractions(likes, scraps);
  }
}
