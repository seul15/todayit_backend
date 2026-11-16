package com.todayit.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 코스와 장소를 분류하는 행정 지역을 나타냅니다. */
@Entity
@Table(name = "region")
public class Region {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "region_id", nullable = false)
  private int regionId;

  @Column(name = "sido_code", nullable = false, length = 5)
  private String sidoCode;

  @Column(name = "sigungu_code", nullable = false, length = 5)
  private String sigunguCode;

  @Column(name = "sido_name", nullable = false, length = 10)
  private String sidoName;

  @Column(name = "sigungu_name", nullable = false, length = 10)
  private String sigunguName;

  protected Region() {}

  /**
   * 행정 지역 정보를 초기화합니다.
   *
   * @param regionId 지역 식별자
   * @param sidoCode 시·도 코드
   * @param sigunguCode 시·군·구 코드
   * @param sidoName 시·도 이름
   * @param sigunguName 시·군·구 이름
   */
  public Region(
      int regionId, String sidoCode, String sigunguCode, String sidoName, String sigunguName) {
    this.regionId = regionId;
    this.sidoCode = sidoCode;
    this.sigunguCode = sigunguCode;
    this.sidoName = sidoName;
    this.sigunguName = sigunguName;
  }

  /**
   * 행정 지역의 조회 스냅샷 정보를 반환합니다.
   *
   * @return 행정 지역 조회 정보 스냅샷
   */
  public RegionSnapshot getSnapshot() {
    return new RegionSnapshot(regionId, sidoCode, sigunguCode, sidoName, sigunguName);
  }

  /**
   * 행정 지역 조회 정보를 담는 불변 레코드입니다.
   *
   * @param regionId 지역 식별자
   * @param sidoCode 시·도 코드
   * @param sigunguCode 시·군·구 코드
   * @param sidoName 시·도 명칭
   * @param sigunguName 시·군·구 명칭
   */
  public record RegionSnapshot(
      int regionId, String sidoCode, String sigunguCode, String sidoName, String sigunguName) {}

  public int getRegionId() {
    return regionId;
  }

  public String getSidoCode() {
    return sidoCode;
  }

  public String getSigunguCode() {
    return sigunguCode;
  }
}
