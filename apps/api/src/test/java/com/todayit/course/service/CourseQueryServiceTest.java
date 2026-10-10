package com.todayit.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.todayit.common.pagination.PageResult;
import com.todayit.course.entity.Course;
import com.todayit.course.entity.CourseTransport;
import com.todayit.course.repository.CourseRepository;
import com.todayit.course.service.model.CourseResult;
import com.todayit.course.service.model.CourseSort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** 장소별 코스 조회 업무를 검증합니다. */
@ExtendWith(MockitoExtension.class)
class CourseQueryServiceTest {

  @Mock private CourseRepository courseRepository;

  @Mock private Course course;

  @Test
  @DisplayName("장소를 포함한 코스를 최신순으로 페이지 조회한다")
  void findsLatestCoursesByPlace() {
    // Given
    Sort expectedSort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("courseId"));
    when(course.getSnapshot())
        .thenReturn(
            new Course.CourseSnapshot(
                1,
                "종로 맛집과 카페 코스",
                "종로 코스",
                CourseTransport.WALKING,
                LocalDateTime.of(2026, 10, 3, 11, 0),
                LocalDateTime.of(2026, 10, 3, 15, 0),
                25));
    when(courseRepository.findPublicActiveByPlaceId(1, PageRequest.of(1, 5, expectedSort)))
        .thenReturn(new PageImpl<>(List.of(course), PageRequest.of(1, 5, expectedSort), 6));
    CourseQueryService service = new CourseQueryService(courseRepository);

    // When
    PageResult<CourseResult> result = service.findCoursesByPlace(1, 1, 5, CourseSort.LATEST);

    // Then
    assertThat(result.content()).hasSize(1);
    assertThat(result.content().getFirst().title()).isEqualTo("종로 맛집과 카페 코스");
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(5);
    assertThat(result.totalElements()).isEqualTo(6);
    verify(courseRepository).findPublicActiveByPlaceId(1, PageRequest.of(1, 5, expectedSort));
  }

  @Test
  @DisplayName("장소를 포함한 코스를 인기순으로 조회한다")
  void findsPopularCoursesByPlace() {
    // Given
    Sort expectedSort = Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("courseId"));
    when(courseRepository.findPublicActiveByPlaceId(
            eq(1),
            argThat(pageable -> pageable != null && pageable.getSort().equals(expectedSort))))
        .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
    CourseQueryService service = new CourseQueryService(courseRepository);

    // When
    PageResult<CourseResult> result = service.findCoursesByPlace(1, 0, 20, CourseSort.POPULAR);

    // Then
    assertThat(result.content()).isEmpty();
    assertThat(result.totalElements()).isZero();
  }
}
