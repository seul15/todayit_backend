package com.todayit.course.entity;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 회원이 만든 코스의 기본 정보와 상태를 나타냅니다. */
@Entity
@Table(name = "course")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "course_id", nullable = false)
  private int courseId;

  @Column(name = "member_id", nullable = false, length = 36)
  private String memberId;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "course_summary", length = 500)
  private String courseSummary;

  @Enumerated(EnumType.STRING)
  @Column(name = "transport", nullable = false, length = 30)
  private CourseTransport transport;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private CourseStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "visibility", nullable = false, length = 20)
  private CourseVisibility visibility;

  @Column(name = "view_count", nullable = false)
  private int viewCount;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<CoursePlace> coursePlaces = new ArrayList<>();

  protected Course() {}

  /**
   * 코스의 조회 정보를 반환합니다.
   *
   * @return 코스 조회 정보
   */
  public CourseSnapshot getSnapshot() {
    return new CourseSnapshot(courseId, title, courseSummary, transport, startAt, endAt, viewCount);
  }

  /**
   * 코스 조회에 필요한 정보를 담습니다.
   *
   * @param courseId 코스 식별자
   * @param title 코스명
   * @param courseSummary 코스 요약
   * @param transport 이동수단
   * @param startAt 코스 시작 시간
   * @param endAt 코스 종료 시간
   * @param viewCount 조회수
   */
  public record CourseSnapshot(
      int courseId,
      String title,
      String courseSummary,
      CourseTransport transport,
      LocalDateTime startAt,
      LocalDateTime endAt,
      int viewCount) {}
}
