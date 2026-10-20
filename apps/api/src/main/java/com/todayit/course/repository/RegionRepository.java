package com.todayit.course.repository;

import com.todayit.course.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 행정 지역 정보를 조회하는 JPA Repository입니다. */
public interface RegionRepository extends JpaRepository<Region, Integer> {

  /**
   * 전달된 코드만 AND 조건으로 적용하여 지역 식별자를 조회합니다.
   *
   * @param si 시·도 코드, 생략 가능
   * @param gun 시·군 코드, 생략 가능
   * @param gu 구 코드, 생략 가능
   * @return 조건에 해당하는 지역 식별자 목록
   */
  @Query(
      "select region.regionId from Region region "
          + "where (:si is null or region.si = :si) "
          + "and (:gun is null or region.gun = :gun) "
          + "and (:gu is null or region.gu = :gu)")
  List<Integer> findIdsByCodes(
      @Param("si") String si, @Param("gun") String gun, @Param("gu") String gu);
}
