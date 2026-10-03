package com.todayit.place.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 장소의 기본 정보와 운영 상태를 나타냅니다. */
@Entity
@Table(name = "place")
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "place_id", nullable = false)
  private int placeId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
  private BigDecimal latitude;

  @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
  private BigDecimal longitude;

  @Column(name = "address", nullable = false, length = 255)
  private String address;

  @Enumerated(EnumType.STRING)
  private Category category;

  @Column(name = "view_count", nullable = false)
  private int viewCount;

  @Column(name = "is_active", nullable = false)
  private boolean isActive;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = true)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PlaceImage> images = new ArrayList<>();

  @OneToMany(mappedBy = "place")
  private List<PlaceScrap> scraps = new ArrayList<>();

  @OneToMany(mappedBy = "place")
  private List<Hours> hours = new ArrayList<>();

  @OneToMany(mappedBy = "place")
  private List<PlaceMemberLike> memberLikes = new ArrayList<>();

  protected Place() {}

  /**
   * 장소에 등록된 이미지를 반환합니다.
   *
   * @return 장소 이미지 목록
   */
  public List<PlaceImage> getImages() {
    return List.copyOf(images);
  }

  /**
   * 장소 식별자를 반환합니다.
   *
   * @return 장소 식별자
   */
  public int getPlaceId() {
    return placeId;
  }

  /**
   * 목록 조회에 필요한 장소 정보를 하나의 값으로 반환합니다.
   *
   * @return 장소 조회 정보
   */
  public PlaceSnapshot getSnapshot() {
    return getSnapshot(images.stream().map(PlaceImage::getImageUrl).toList());
  }

  /**
   * 배치 조회한 이미지 URL을 사용해 장소 목록 정보를 반환합니다.
   *
   * @param imageUrls 장소 이미지 URL 목록
   * @return 장소 조회 정보
   */
  public PlaceSnapshot getSnapshot(List<String> imageUrls) {
    return new PlaceSnapshot(
        placeId, name, latitude, longitude, address, category, viewCount, List.copyOf(imageUrls));
  }

  /**
   * 장소 목록에 공개할 정보를 담습니다.
   *
   * @param placeId 장소 식별자
   * @param name 장소명
   * @param latitude 위도
   * @param longitude 경도
   * @param address 주소
   * @param category 카테고리
   * @param viewCount 조회수
   * @param imageUrls 장소 이미지 URL 목록
   */
  public record PlaceSnapshot(
      int placeId,
      String name,
      BigDecimal latitude,
      BigDecimal longitude,
      String address,
      Category category,
      int viewCount,
      List<String> imageUrls) {}
}
