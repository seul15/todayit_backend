package com.todayit.place.repository;

import com.todayit.place.entity.PlaceImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 장소 이미지 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface PlaceImageRepository extends JpaRepository<PlaceImage, Integer> {

  /**
   * 장소의 이미지를 페이지 단위로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @param pageable 페이지와 정렬 조건
   * @return 장소 이미지 페이지
   */
  Page<PlaceImage> findByPlacePlaceId(int placeId, Pageable pageable);

  /**
   * 여러 장소의 이미지를 한 번에 조회합니다.
   *
   * @param placeIds 장소 식별자 목록
   * @return 장소 이미지 목록
   */
  List<PlaceImage> findByPlacePlaceIdInOrderByPlacePlaceIdAscCreatedAtAsc(
      Collection<Integer> placeIds);
}
