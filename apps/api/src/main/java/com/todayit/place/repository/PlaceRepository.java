package com.todayit.place.repository;

import com.todayit.place.entity.Place;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 장소 정보를 조회하고 저장하는 JPA Repository입니다. */
public interface PlaceRepository extends JpaRepository<Place, Integer> {

  /**
   * 활성화되고 삭제되지 않은 장소를 페이지 단위로 조회합니다.
   *
   * @param pageable 페이지와 정렬 조건
   * @return 조건에 맞는 장소 페이지
   */
  Page<Place> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);

  /**
   * 활성화되고 삭제되지 않은 장소를 식별자로 조회합니다.
   *
   * @param placeId 장소 식별자
   * @return 조건에 맞는 장소
   */
  Optional<Place> findByPlaceIdAndIsActiveTrueAndIsDeletedFalse(int placeId);
}
