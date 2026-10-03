package com.todayit.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 장소 이미지의 URL과 생성·수정 시각을 나타냅니다. */
@Entity
@Table(name = "place_image")
public class PlaceImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "place_image_id", nullable = false)
  private int placeImageId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "image_url", nullable = false, length = 2048)
  private String imageUrl;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected PlaceImage() {}

  /**
   * 장소 이미지 식별자를 반환합니다.
   *
   * @return 장소 이미지 식별자
   */
  public int getPlaceImageId() {
    return placeImageId;
  }

  /**
   * 이미지가 등록된 장소 식별자를 반환합니다.
   *
   * @return 장소 식별자
   */
  public int getPlaceId() {
    return place.getPlaceId();
  }

  /**
   * 장소 이미지 URL을 반환합니다.
   *
   * @return 장소 이미지 URL
   */
  public String getImageUrl() {
    return imageUrl;
  }

  /**
   * 생성 시각을 반환합니다.
   *
   * @return 생성 시각
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  /**
   * 수정 시각을 반환합니다.
   *
   * @return 수정 시각
   */
  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
