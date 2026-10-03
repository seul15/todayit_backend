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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** 회원이 스크랩한 장소 정보를 나타냅니다. */
@Entity
@Table(
    name = "place_scrap",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_place_scrap_member_place",
            columnNames = {"member_id", "place_id"}))
public class PlaceScrap {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "place_scrap_id", nullable = false)
  private int placeScrapId;

  @Column(name = "member_id", nullable = false, length = 36)
  private String memberId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "is_deleted", nullable = false)
  private boolean isDeleted;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  protected PlaceScrap() {}

  /**
   * 회원의 장소 스크랩을 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param place 스크랩할 장소
   */
  private PlaceScrap(String memberId, Place place) {
    this.memberId = memberId;
    this.place = place;
    this.isDeleted = false;
    this.createdAt = LocalDateTime.now();
  }

  /**
   * 회원의 장소 스크랩을 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param place 스크랩할 장소
   * @return 장소 스크랩
   */
  public static PlaceScrap create(String memberId, Place place) {
    return new PlaceScrap(memberId, place);
  }
}
