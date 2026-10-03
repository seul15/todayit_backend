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

/** 장소의 영업시간 정보를 나타냅니다. */
@Entity
@Table(name = "hours")
public class Hours {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "hours_id", nullable = false)
  private int hoursId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "place_id", nullable = false)
  private Place place;

  @Column(name = "open_date", nullable = false)
  private LocalDateTime openDate;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Column(name = "break_start_at")
  private LocalDateTime breakStartAt;

  @Column(name = "break_end_at")
  private LocalDateTime breakEndAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected Hours() {}

  /**
   * 영업시간 식별자를 반환합니다.
   *
   * @return 영업시간 식별자
   */
  public int getHoursId() {
    return hoursId;
  }

  /**
   * 영업 날짜를 반환합니다.
   *
   * @return 영업 날짜
   */
  public LocalDateTime getOpenDate() {
    return openDate;
  }

  /**
   * 영업 시작 시각을 반환합니다.
   *
   * @return 영업 시작 시각
   */
  public LocalDateTime getStartAt() {
    return startAt;
  }

  /**
   * 영업 종료 시각을 반환합니다.
   *
   * @return 영업 종료 시각
   */
  public LocalDateTime getEndAt() {
    return endAt;
  }

  /**
   * 브레이크 시작 시각을 반환합니다.
   *
   * @return 브레이크 시작 시각
   */
  public LocalDateTime getBreakStartAt() {
    return breakStartAt;
  }

  /**
   * 브레이크 종료 시각을 반환합니다.
   *
   * @return 브레이크 종료 시각
   */
  public LocalDateTime getBreakEndAt() {
    return breakEndAt;
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
