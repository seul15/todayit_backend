package com.todayit.course.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.todayit.common.exception.GlobalExceptionHandler;
import com.todayit.course.service.RegionQueryService;
import com.todayit.course.service.model.RegionResult;
import com.todayit.course.service.model.SigunguResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RegionControllerTest {

  @Mock private RegionQueryService regionQueryService;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    RegionController controller = new RegionController(regionQueryService);
    mockMvc =
        MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  @DisplayName("전체 지역 목록을 계층 구조로 조회한다")
  void getsAllRegionsHierarchically() throws Exception {
    // Given
    List<RegionResult> results =
        List.of(
            new RegionResult(
                "11",
                "서울특별시",
                List.of(new SigunguResult("110", "종로구"), new SigunguResult("680", "강남구"))),
            new RegionResult("26", "부산광역시", List.of(new SigunguResult("110", "중구"))));
    when(regionQueryService.findAllRegions()).thenReturn(results);

    // When & Then
    mockMvc
        .perform(get("/api/v1/regions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].sidoCode").value("11"))
        .andExpect(jsonPath("$.data[0].sidoName").value("서울특별시"))
        .andExpect(jsonPath("$.data[0].sigungus[0].sigunguCode").value("110"))
        .andExpect(jsonPath("$.data[0].sigungus[0].sigunguName").value("종로구"))
        .andExpect(jsonPath("$.data[0].sigungus[1].sigunguCode").value("680"))
        .andExpect(jsonPath("$.data[0].sigungus[1].sigunguName").value("강남구"))
        .andExpect(jsonPath("$.data[1].sidoCode").value("26"))
        .andExpect(jsonPath("$.data[1].sidoName").value("부산광역시"))
        .andExpect(jsonPath("$.data[1].sigungus[0].sigunguCode").value("110"))
        .andExpect(jsonPath("$.data[1].sigungus[0].sigunguName").value("중구"));
  }

  @Test
  @DisplayName("지역 데이터가 없으면 빈 배열을 반환한다")
  void returnsEmptyListWhenNoRegions() throws Exception {
    // Given
    when(regionQueryService.findAllRegions()).thenReturn(List.of());

    // When & Then
    mockMvc
        .perform(get("/api/v1/regions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());
  }
}
