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

/** 회원이 좋아요를 누른 장소 정보를 나타냅니다. */
@Entity
@Table(name = "place_member_like")
public class PlaceMemberLike {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "place_member_like_id", nullable = false)
  private int placeMemberLikeId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "member_id", nullable = false, length = 36)
  private String memberId;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  protected PlaceMemberLike() {}

  /**
   * 장소 회원 좋아요 식별자를 반환합니다.
   *
   * @return 장소 회원 좋아요 식별자
   */
  public int getPlaceMemberLikeId() {
    return placeMemberLikeId;
  }

  /**
   * 회원 식별자를 반환합니다.
   *
   * @return 회원 식별자
   */
  public String getMemberId() {
    return memberId;
  }

  /**
   * 생성 시각을 반환합니다.
   *
   * @return 생성 시각
   */
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
