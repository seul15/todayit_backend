package com.todayit.place.service;

import com.todayit.place.entity.Hours;
import com.todayit.place.entity.Place;
import com.todayit.place.exception.PlaceNotFoundException;
import com.todayit.place.repository.PlaceMemberLikeRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceDetailResult;
import com.todayit.place.service.model.PlaceOpeningHoursResult;
import com.todayit.place.service.model.PlaceResult;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 활성 장소의 상세 정보와 회원별 상태를 조회합니다. */
@Service
public class PlaceDetailQueryService {
  private final PlaceRepository places;
  private final PlaceMemberLikeRepository likes;
  private final PlaceScrapRepository scraps;

  /**
   * 상세 조회에 필요한 장소 저장소를 주입합니다.
   *
   * @param places 장소 조회
   * @param likes 좋아요 저장소
   * @param scraps 스크랩 저장소
   */
  public PlaceDetailQueryService(
      PlaceRepository places, PlaceMemberLikeRepository likes, PlaceScrapRepository scraps) {
    this.places = places;
    this.likes = likes;
    this.scraps = scraps;
  }

  /**
   * 장소 상세를 조회하며 비회원의 좋아요와 스크랩은 false로 반환합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자, 비회원이면 null
   * @return 조회 결과
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태인 경우
   */
  @Transactional(readOnly = true)
  public PlaceDetailResult getPlaceDetail(int placeId, String memberId) {
    Place place =
        places
            .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
            .orElseThrow(PlaceNotFoundException::new);
    return new PlaceDetailResult(
        PlaceResult.from(place.getSnapshot()),
        place.getDescription(),
        place.getHours().stream()
            .sorted(
                Comparator.comparing(Hours::getOpenDate)
                    .thenComparing(Hours::getStartAt)
                    .thenComparingInt(Hours::getHoursId))
            .map(
                hour ->
                    new PlaceOpeningHoursResult(
                        hour.getOpenDate(),
                        hour.getStartAt(),
                        hour.getEndAt(),
                        hour.getBreakStartAt(),
                        hour.getBreakEndAt()))
            .toList(),
        memberId != null && likes.existsByMemberIdAndPlacePlaceId(memberId, placeId),
        memberId != null
            && scraps.existsByMemberIdAndPlacePlaceIdAndIsDeletedFalse(memberId, placeId));
  }
}
