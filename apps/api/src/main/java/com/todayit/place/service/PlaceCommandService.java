package com.todayit.place.service;

import com.todayit.place.exception.PlaceAlreadyLikedException;
import com.todayit.place.exception.PlaceAlreadyScrappedException;
import com.todayit.place.exception.PlaceNotFoundException;
import com.todayit.place.repository.PlaceMemberLikeRepository;
import com.todayit.place.repository.PlaceRepository;
import com.todayit.place.repository.PlaceScrapRepository;
import com.todayit.place.service.model.PlaceLikeResult;
import com.todayit.place.service.model.PlaceScrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 장소 변경 업무를 처리하는 Command Service입니다. */
@Service
public class PlaceCommandService {

  private static final Logger LOG = LoggerFactory.getLogger(PlaceCommandService.class);

  private final PlaceRepository placeRepository;
  private final PlaceMemberLikeRepository placeMemberLikeRepository;
  private final PlaceScrapRepository placeScrapRepository;

  /**
   * 장소 변경에 필요한 Repository를 받습니다.
   *
   * @param placeRepository 장소 Repository
   * @param placeMemberLikeRepository 장소 좋아요 Repository
   * @param placeScrapRepository 장소 스크랩 Repository
   */
  public PlaceCommandService(
      PlaceRepository placeRepository,
      PlaceMemberLikeRepository placeMemberLikeRepository,
      PlaceScrapRepository placeScrapRepository) {
    this.placeRepository = placeRepository;
    this.placeMemberLikeRepository = placeMemberLikeRepository;
    this.placeScrapRepository = placeScrapRepository;
  }

  /**
   * 회원의 장소 스크랩을 생성하고 장소의 활성 스크랩 수를 반환합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자
   * @return 장소 스크랩 결과
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   * @throws PlaceAlreadyScrappedException 이미 스크랩한 장소일 때
   */
  @Transactional
  public PlaceScrapResult scrapPlace(int placeId, String memberId) {
    LOG.info("장소 스크랩 요청: placeId={}", placeId);
    validatePlace(placeId);
    if (placeScrapRepository.existsByMemberIdAndPlacePlaceIdAndIsDeletedFalse(memberId, placeId)) {
      throw new PlaceAlreadyScrappedException();
    }
    int upsertedCount = placeScrapRepository.upsertByMemberIdAndPlaceId(memberId, placeId);
    if (upsertedCount == 0) {
      throw new PlaceAlreadyScrappedException();
    }

    long scrapCount = placeScrapRepository.countActiveByPlaceId(placeId);
    LOG.info("장소 스크랩 처리: placeId={}, scrapCount={}", placeId, scrapCount);
    return new PlaceScrapResult(placeId, true, scrapCount);
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
    LOG.info("장소 스크랩 취소 요청: placeId={}", placeId);
    validatePlace(placeId);
    int cancelledCount = placeScrapRepository.cancelByMemberIdAndPlaceId(memberId, placeId);

    long scrapCount = placeScrapRepository.countActiveByPlaceId(placeId);
    LOG.info(
        "장소 스크랩 취소 처리: placeId={}, cancelledCount={}, scrapCount={}",
        placeId,
        cancelledCount,
        scrapCount);
    return new PlaceScrapResult(placeId, false, scrapCount);
  }

  /**
   * 회원의 장소 좋아요를 생성하고 장소의 좋아요 수를 반환합니다.
   *
   * @param placeId 장소 식별자
   * @param memberId 회원 식별자
   * @return 장소 좋아요 결과
   * @throws PlaceNotFoundException 장소가 없거나 비활성·삭제 상태일 때
   * @throws PlaceAlreadyLikedException 이미 좋아요를 누른 장소일 때
   */
  @Transactional
  public PlaceLikeResult likePlace(int placeId, String memberId) {
    LOG.info("장소 좋아요 요청: placeId={}", placeId);
    validatePlace(placeId);
    int insertedCount = placeMemberLikeRepository.insertIfAbsent(memberId, placeId);
    if (insertedCount == 0) {
      throw new PlaceAlreadyLikedException();
    }

    long likeCount = placeMemberLikeRepository.countByPlaceId(placeId);
    LOG.info("장소 좋아요 처리: placeId={}, likeCount={}", placeId, likeCount);
    return new PlaceLikeResult(placeId, true, likeCount);
  }

  private void validatePlace(int placeId) {
    placeRepository
        .findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(placeId)
        .orElseThrow(PlaceNotFoundException::new);
  }
}
