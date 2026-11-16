package com.todayit.course.controller;

import com.todayit.common.response.CommonResponse;
import com.todayit.course.controller.docs.RegionApiDocs;
import com.todayit.course.dto.response.RegionResponse;
import com.todayit.course.service.RegionQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 행정 지역 관련 HTTP 요청을 처리하는 Controller입니다. */
@RestController
@RequestMapping("/api/v1/regions")
public class RegionController implements RegionApiDocs {

  private final RegionQueryService regionQueryService;

  /**
   * 지역 조회를 처리할 서비스를 받습니다.
   *
   * @param regionQueryService 행정 지역 조회 Service
   */
  public RegionController(RegionQueryService regionQueryService) {
    this.regionQueryService = regionQueryService;
  }

  /**
   * 전체 행정 지역 목록을 시·도 및 시·군·구 계층 구조로 조회합니다.
   *
   * @return 시·도 및 하위 시·군·구 목록
   */
  @GetMapping
  @Override
  public ResponseEntity<CommonResponse<List<RegionResponse>>> getRegions() {
    List<RegionResponse> regions =
        regionQueryService.findAllRegions().stream().map(RegionResponse::from).toList();
    return ResponseEntity.ok(CommonResponse.success(regions));
  }
}
