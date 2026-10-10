package com.todayit.place.repository;

import com.todayit.place.entity.Place;
import com.todayit.place.entity.PlaceScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 장소 스크랩 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface PlaceScrapRepository extends JpaRepository<PlaceScrap, Integer> {

  /**
   * 회원이 장소를 활성 상태로 스크랩했는지 확인합니다.
   *
   * @param memberId 회원 식별자
   * @param placeId 장소 식별자
   * @return 활성 스크랩 존재 여부
   */
  boolean existsByMemberIdAndPlacePlaceIdAndIsDeletedFalse(String memberId, int placeId);

  /**
   * 회원이 활성 상태로 스크랩한 장소를 오래된 순서로 조회합니다.
   *
   * @param memberId 회원 식별자
   * @param pageable 페이지 정보
   * @return 스크랩 장소 페이지
   */
  @Query(
      value =
          "select scrap.place from PlaceScrap scrap "
              + "where scrap.memberId = :memberId "
              + "and scrap.isDeleted = false "
              + "and scrap.place.isActive = true "
              + "and scrap.place.isDeleted = false "
              + "order by coalesce(scrap.updatedAt, scrap.createdAt) asc, scrap.place.placeId asc",
      countQuery =
          "select count(scrap) from PlaceScrap scrap "
              + "where scrap.memberId = :memberId "
              + "and scrap.isDeleted = false "
              + "and scrap.place.isActive = true "
              + "and scrap.place.isDeleted = false")
  Page<Place> findScrappedPlacesOldest(@Param("memberId") String memberId, Pageable pageable);

  /**
   * 회원이 활성 상태로 스크랩한 장소를 최신 순서로 조회합니다.
   *
   * @param memberId 회원 식별자
   * @param pageable 페이지 정보
   * @return 스크랩 장소 페이지
   */
  @Query(
      value =
          "select scrap.place from PlaceScrap scrap "
              + "where scrap.memberId = :memberId "
              + "and scrap.isDeleted = false "
              + "and scrap.place.isActive = true "
              + "and scrap.place.isDeleted = false "
              + "order by coalesce(scrap.updatedAt, scrap.createdAt) desc, scrap.place.placeId desc",
      countQuery =
          "select count(scrap) from PlaceScrap scrap "
              + "where scrap.memberId = :memberId "
              + "and scrap.isDeleted = false "
              + "and scrap.place.isActive = true "
              + "and scrap.place.isDeleted = false")
  Page<Place> findScrappedPlacesLatest(@Param("memberId") String memberId, Pageable pageable);

  /**
   * 회원의 장소 스크랩을 생성하거나 기존 스크랩을 활성화합니다.
   *
   * @param memberId 회원 식별자
   * @param placeId 장소 식별자
   * @return 반영된 스크랩 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          "insert into place_scrap "
              + "(member_id, place_id, is_deleted, created_at) "
              + "values (:memberId, :placeId, false, current_timestamp) "
              + "on conflict (member_id, place_id) do update set "
              + "is_deleted = false, deleted_at = null, updated_at = current_timestamp "
              + "where place_scrap.is_deleted = true",
      nativeQuery = true)
  int upsertByMemberIdAndPlaceId(@Param("memberId") String memberId, @Param("placeId") int placeId);

  /**
   * 회원의 장소 스크랩을 취소합니다.
   *
   * @param memberId 회원 식별자
   * @param placeId 장소 식별자
   * @return 취소된 스크랩 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update PlaceScrap scrap "
          + "set scrap.isDeleted = true, scrap.deletedAt = CURRENT_TIMESTAMP, "
          + "scrap.updatedAt = CURRENT_TIMESTAMP "
          + "where scrap.memberId = :memberId "
          + "and scrap.place.placeId = :placeId "
          + "and scrap.isDeleted = false")
  int cancelByMemberIdAndPlaceId(@Param("memberId") String memberId, @Param("placeId") int placeId);

  /**
   * 장소의 활성 스크랩 수를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 활성 스크랩 수
   */
  @Query(
      "select count(scrap) from PlaceScrap scrap "
          + "where scrap.place.placeId = :placeId and scrap.isDeleted = false")
  long countActiveByPlaceId(@Param("placeId") int placeId);
}
