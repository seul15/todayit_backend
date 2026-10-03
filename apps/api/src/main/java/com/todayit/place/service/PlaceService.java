package com.todayit.place.service;

import com.todayit.common.pagination.PageResult;
import com.todayit.place.entity.Place;
import com.todayit.place.entity.PlaceImage;
import com.todayit.place.exception.PlaceNotFoundException;
import com.todayit.place.repository.PlaceImageRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceImageResult;
import com.todayit.place.service.model.PlaceLocationResult;
import com.todayit.place.service.model.PlaceResult;
import com.todayit.place.service.model.PlaceScrapResult;
import com.todayit.place.service.model.PlaceSort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 관련 업무를 처리하는 Service입니다. */
@Service
public class PlaceService {

  private final PlaceRepository placeRepository;
  private final PlaceImageRepository placeImageRepository;
  private final PlaceScrapRepository placeScrapRepository;

  /**
   * 장소 정보를 조회하고 저장할 Repository를 받습니다.
   *
   * @param placeRepository 장소 Repository
   * @param placeImageRepository 장소 이미지 Repository
   * @param placeScrapRepository 장소 스크랩 Repository
   */
  public PlaceService(
      PlaceRepository placeRepository,
      PlaceImageRepository placeImageRepository,
      PlaceScrapRepository placeScrapRepository) {
    this.placeRepository = placeRepository;
    this.placeImageRepository = placeImageRepository;
    this.placeScrapRepository = placeScrapRepository;
  }

  /**
   * 활성화되고 삭제되지 않은 장소를 페이지 단위로 조회합니다. 장소 이미지도 페이지 내 장소를 기준으로 일괄 조회하여 결과에 포함합니다.
   *
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @param sort 정렬 기준
   * @return 장소 목록과 페이지 정보
   */
  @Transactional(readOnly = true)
  public PageResult<PlaceResult> findPlaces(int page, int size, PlaceSort sort) {
    Sort order =
        sort == PlaceSort.LATEST
            ? Sort.by(Sort.Direction.DESC, "createdAt")
            : Sort.by(Sort.Direction.DESC, "viewCount");

    Page<Place> placePage =
        placeRepository.findByIsActiveTrueAndIsDeletedFalse(PageRequest.of(page, size, order));
    List<Place> places = placePage.getContent();
    Map<Integer, List<String>> imageUrlsByPlaceId = findImageUrlsByPlaceId(places);
    List<PlaceResult> content =
        places.stream().map(place -> toResult(place, imageUrlsByPlaceId)).toList();

    return new PageResult<>(
        content, placePage.getNumber(), placePage.getSize(), placePage.getTotalElements());
  }

  /**
   * 여러 장소의 이미지 URL을 장소 식별자별로 그룹화합니다. 빈 장소 목록이면 이미지 Repository를 호출하지 않습니다.
   *
   * @param places 이미지 URL을 조회할 장소 목록
   * @return 장소 식별자별 이미지 URL 목록
   */
  private Map<Integer, List<String>> findImageUrlsByPlaceId(List<Place> places) {
    if (places.isEmpty()) {
      return Map.of();
    }

    return placeImageRepository
        .findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(
            places.stream().map(Place::getPlaceId).toList())
        .stream()
        .collect(
            Collectors.groupingBy(
                PlaceImage::getPlaceId,
                Collectors.mapping(PlaceImage::getImageUrl, Collectors.toList())));
  }

  /**
   * 배치 조회한 이미지 URL을 포함해 장소 Entity를 서비스 결과로 변환합니다.
   *
   * @param place 변환할 장소
   * @param imageUrlsByPlaceId 장소 식별자별 이미지 URL 목록
   * @return 장소 서비스 결과
   */
  private PlaceResult toResult(Place place, Map<Integer, List<String>> imageUrlsByPlaceId) {
    return PlaceResult.from(
        place.getSnapshot(imageUrlsByPlaceId.getOrDefault(place.getPlaceId(), List.of())));
  }

  /**
   * 활성화되고 삭제되지 않은 장소의 지도 정보를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 장소 식별자, 좌표와 주소
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   */
  @Transactional(readOnly = true)
  public PlaceLocationResult findPlaceLocation(int placeId) {

    Place place =
        placeRepository
            .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
            .orElseThrow(PlaceNotFoundException::new);
    Place.PlaceSnapshot snapshot = place.getSnapshot();

    return PlaceLocationResult.from(snapshot);
  }

  /**
   * 활성화되고 삭제되지 않은 장소의 사진을 최신 등록순으로 페이지 단위 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param page 페이지 번호
   * @param size 페이지 크기
   * @return 장소 사진 목록과 페이지 정보
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   */
  @Transactional(readOnly = true)
  public PageResult<PlaceImageResult> findPlaceImages(int placeId, int page, int size) {

    placeRepository
        .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
        .orElseThrow(PlaceNotFoundException::new);

    Page<PlaceImage> imagePage =
        placeImageRepository.findByPlacePlaceId(
            placeId,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("placeImageId"))));

    return new PageResult<>(
        imagePage.getContent().stream()
            .map(image -> new PlaceImageResult(image.getPlaceImageId(), image.getImageUrl()))
            .toList(),
        imagePage.getNumber(),
        imagePage.getSize(),
        imagePage.getTotalElements());
  }

  /**
   * 회원의 장소 스크랩을 생성하고 장소의 활성 스크랩 수를 반환합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자
   * @return 장소 스크랩 결과
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   */
  @Transactional
  public PlaceScrapResult scrapPlace(int placeId, String memberId) {
    placeRepository
        .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
        .orElseThrow(PlaceNotFoundException::new);

    placeScrapRepository.upsertByMemberIdAndPlaceId(memberId, placeId);

    return new PlaceScrapResult(placeId, true, placeScrapRepository.countActiveByPlaceId(placeId));
  }

  /**
   * 회원의 장소 스크랩을 취소하고 장소의 활성 스크랩 수를 반환합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자
   * @return 장소 스크랩 결과
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   */
  @Transactional
  public PlaceScrapResult cancelPlaceScrap(int placeId, String memberId) {
    placeRepository
        .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
        .orElseThrow(PlaceNotFoundException::new);

    placeScrapRepository.cancelByMemberIdAndPlaceId(memberId, placeId);

    return new PlaceScrapResult(placeId, false, placeScrapRepository.countActiveByPlaceId(placeId));
  }

  /**
   * 장소 Entity를 목록 조회 결과로 변환합니다.
   *
   * @param place 장소 Entity
   * @return 장소 조회 결과
   */
}
