package com.todayit.place.repository;

import com.todayit.place.entity.PlaceMemberLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 장소 좋아요 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface PlaceMemberLikeRepository extends JpaRepository<PlaceMemberLike, Integer> {

  /**
   * 회원의 장소 좋아요를 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param placeId 장소 식별자
   * @return 반영된 좋아요 수
   */
  @Modifying
  @Query(
      value =
          "insert into place_member_like (member_id, place_id, created_at) "
              + "values (:memberId, :placeId, current_timestamp) "
              + "on conflict (member_id, place_id) do nothing",
      nativeQuery = true)
  int insertIfAbsent(@Param("memberId") String memberId, @Param("placeId") int placeId);

  /**
   * 장소의 좋아요 수를 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 장소 좋아요 수
   */
  @Query(
      "select count(memberLike) from PlaceMemberLike memberLike "
          + "where memberLike.place.placeId = :placeId")
  long countByPlaceId(@Param("placeId") int placeId);
}
