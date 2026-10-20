package com.todayit.place.dto.response;

import com.todayit.place.entity.Category;
import com.todayit.place.service.model.PlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 장소 목록에서 반환할 장소 응답입니다.
 *
 * @param placeId 장소 식별자
 * @param name 장소명
 * @param latitude 위도
 * @param longitude 경도
 * @param address 주소
 * @param category 카테고리
 * @param viewCount 조회수
 * @param liked 요청 회원의 좋아요 여부, 비회원이면 false
 * @param imageUrls 장소 이미지 URL 목록
 */
@Schema(description = "장소 목록 응답의 장소 정보")
public record PlaceFilterResponse(
    @Schema(description = "장소 식별자입니다.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int placeId,
    @Schema(
            description = "장소 이름입니다.",
            example = "오늘 카페",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
    @Schema(
            description = "장소의 위도입니다.",
            example = "37.5665",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal latitude,
    @Schema(
            description = "장소의 경도입니다.",
            example = "126.9780",
            requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal longitude,
    @Schema(
            description = "장소 주소입니다.",
            example = "서울특별시 중구 세종대로 110",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
    @Schema(
            description = "장소의 이용 목적과 성격을 구분하는 카테고리입니다.",
            example = "CAFE_DESSERT",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Category category,
    @Schema(description = "장소 조회수입니다.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        int viewCount,
    @Schema(
            description = "장소 이미지 URL 목록입니다. 이미지가 없으면 빈 목록을 반환합니다.",
            example = "[\"https://example.com/images/place-1.jpg\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> imageUrls,
    @Schema(
            description = "요청 회원의 좋아요 여부입니다. 비회원이면 false입니다.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean liked) {

  /**
   * 장소 조회 결과를 API 응답으로 변환합니다.
   *
   * @param result 장소 조회 결과
   * @return 장소 응답
   */
  public static PlaceFilterResponse from(PlaceResult result) {
    return new PlaceFilterResponse(
        result.placeId(),
        result.name(),
        result.latitude(),
        result.longitude(),
        result.address(),
        result.category(),
        result.viewCount(),
        result.imageUrls(),
        result.liked());
  }
}
