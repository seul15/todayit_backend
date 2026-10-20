package com.todayit.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.course.exception.InvalidRegionPathException;
import com.todayit.course.repository.RegionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionQueryServiceTest {
  @Mock private RegionRepository regionRepository;

  @Test
  @DisplayName("시도 코드만으로 조회한다")
  void findsRegionsBySiOnly() {
    // Given
    when(regionRepository.findIdsByCodes("41", null, null)).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds("41", null, null);

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("시군 코드만으로 조회한다")
  void findsRegionsByGunOnly() {
    // Given
    when(regionRepository.findIdsByCodes(null, "110", null)).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds(null, "110", null);

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("구 코드만으로 조회한다")
  void findsRegionsByGuOnly() {
    // Given
    when(regionRepository.findIdsByCodes(null, null, "111")).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds(null, null, "111");

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("시도와 시군 코드를 함께 적용한다")
  void findsRegionsBySiAndGun() {
    // Given
    when(regionRepository.findIdsByCodes("41", "110", null)).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds("41", "110", null);

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("시도와 구 코드를 함께 적용한다")
  void findsRegionsBySiAndGu() {
    // Given
    when(regionRepository.findIdsByCodes("41", null, "111")).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds("41", null, "111");

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("시군과 구 코드를 함께 적용한다")
  void findsRegionsByGunAndGu() {
    // Given
    when(regionRepository.findIdsByCodes(null, "110", "111")).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds(null, "110", "111");

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("지역 코드 세 조건을 모두 적용한다")
  void findsRegionsByAllCodes() {
    // Given
    when(regionRepository.findIdsByCodes("41", "110", "111")).thenReturn(List.of(1, 2));
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When
    List<Integer> result = service.findRegionIds("41", "110", "111");

    // Then
    assertThat(result).containsExactly(1, 2);
  }

  @Test
  @DisplayName("시도 코드가 2자리보다 짧으면 거부한다")
  void rejectsShortSi() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds("1", null, null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("시도 코드가 2자리보다 길면 거부한다")
  void rejectsLongSi() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds("411", null, null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("시도 코드가 숫자가 아니면 거부한다")
  void rejectsNonNumericSi() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds("ab", null, null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("공백 시도 코드를 거부한다")
  void rejectsBlankSi() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(" ", null, null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("빈 시도 코드를 거부한다")
  void rejectsEmptySi() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds("", null, null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("시군 코드가 3자리보다 짧으면 거부한다")
  void rejectsShortGun() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, "11", null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("시군 코드가 3자리보다 길면 거부한다")
  void rejectsLongGun() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, "1100", null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("시군 코드가 숫자가 아니면 거부한다")
  void rejectsNonNumericGun() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, "abc", null))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("구 코드가 3자리보다 짧으면 거부한다")
  void rejectsShortGu() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, null, "11"))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("구 코드가 3자리보다 길면 거부한다")
  void rejectsLongGu() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, null, "1111"))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("구 코드가 숫자가 아니면 거부한다")
  void rejectsNonNumericGu() {
    // Given
    RegionQueryService service = new RegionQueryService(regionRepository);

    // When & Then
    assertThatThrownBy(() -> service.findRegionIds(null, null, "abc"))
        .isInstanceOf(InvalidRegionPathException.class);
    verifyNoInteractions(regionRepository);
  }

  @Test
  @DisplayName("존재하지 않는 지역 코드 조합을 거부한다")
  void rejectsUnknownCodeCombination() {
    when(regionRepository.findIdsByCodes("41", "999", null)).thenReturn(List.of());
    assertThatThrownBy(
            () -> new RegionQueryService(regionRepository).findRegionIds("41", "999", null))
        .isInstanceOf(InvalidRegionPathException.class);
  }
}
