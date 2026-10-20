package com.todayit.place.dto.response;

import com.todayit.place.entity.Category;
import com.todayit.place.service.model.PlaceDetailResult;
import com.todayit.place.service.model.PlaceDetailWithStayResult;
import com.todayit.place.service.model.PlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 장소 상세 응답입니다.
 *
 * @param placeId 장소 식별자
 * @param name 장소명
 * @param category 카테고리
 * @param address 주소
 * @param location 좌표
 * @param description 장소 설명
 * @param openingHours 영업시간 목록
 * @param images 이미지 URL 목록
 * @param averageStayMinutes 평균 체류 시간(분), 데이터가 없으면 0
 * @param liked 좋아요 여부
 * @param scrapped 스크랩 여부
 */
@Schema(description = "장소 상세 응답")
public record PlaceDetailResponse(
    @Schema(description = "장소 식별자입니다.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        int placeId,
    @Schema(
            description = "장소 이름입니다.",
            example = "오늘 카페",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String name,
    @Schema(
            description = "장소의 이용 목적과 성격을 구분하는 카테고리입니다.",
            example = "CAFE_DESSERT",
            requiredMode = Schema.RequiredMode.REQUIRED)
        Category category,
    @Schema(
            description = "장소 주소입니다.",
            example = "서울특별시 종로구 종로 1",
            requiredMode = Schema.RequiredMode.REQUIRED)
        String address,
    @Schema(description = "장소의 위도와 경도입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        Location location,
    @Schema(
            description = "장소 설명입니다. 등록된 설명이 없으면 null을 반환합니다.",
            example = "조용한 분위기에서 커피를 즐길 수 있는 카페입니다.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true)
        String description,
    @Schema(
            description = "영업 날짜와 시작 시각 순으로 정렬한 영업시간 목록입니다. 정보가 없으면 빈 목록을 반환합니다.",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<PlaceOpeningHoursResponse> openingHours,
    @Schema(
            description = "장소 이미지 URL 목록입니다. 이미지가 없으면 빈 목록을 반환합니다.",
            example = "[\"https://example.com/images/place-1.jpg\"]",
            requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> images,
    @Schema(
            description =
                "공개 여부와 관계없이 해당 장소가 포함된 활성 코스에서 0분을 제외한 체류 시간 평균을 반올림한 분 단위 값입니다. 유효한 데이터가 없으면 0을 반환합니다.",
            example = "60",
            requiredMode = Schema.RequiredMode.REQUIRED)
        long averageStayMinutes,
    @Schema(
            description = "현재 회원이 이 장소를 좋아요 했는지 나타냅니다. 비회원이면 false를 반환합니다.",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean liked,
    @Schema(
            description = "현재 회원이 이 장소를 스크랩했는지 나타냅니다. 비회원이면 false를 반환합니다.",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED)
        boolean scrapped) {
  /**
   * 응답의 영업시간과 이미지 목록을 방어적으로 복사합니다.
   *
   * @param placeId 장소 식별자
   * @param name 장소명
   * @param category 카테고리
   * @param address 주소
   * @param location 좌표
   * @param description 장소 설명
   * @param openingHours 영업시간 목록
   * @param images 이미지 URL 목록
   * @param averageStayMinutes 평균 체류 시간(분), 데이터가 없으면 0
   * @param liked 좋아요 여부
   * @param scrapped 스크랩 여부
   */
  public PlaceDetailResponse {
    openingHours = List.copyOf(openingHours);
    images = List.copyOf(images);
  }

  /**
   * 서비스 결과를 장소 상세 응답으로 변환합니다.
   *
   * @param result 상세 조회 결과
   * @return 조회 결과
   */
  public static PlaceDetailResponse from(PlaceDetailWithStayResult result) {
    PlaceDetailResult detail = result.detail();
    PlaceResult place = detail.place();
    return new PlaceDetailResponse(
        place.placeId(),
        place.name(),
        place.category(),
        place.address(),
        new Location(place.latitude(), place.longitude()),
        detail.description(),
        detail.openingHours().stream()
            .map(
                h ->
                    new PlaceOpeningHoursResponse(
                        h.openDate(), h.startAt(), h.endAt(), h.breakStartAt(), h.breakEndAt()))
            .toList(),
        place.imageUrls(),
        result.averageStayMinutes(),
        detail.liked(),
        detail.scrapped());
  }

  /**
   * 장소의 위도와 경도입니다.
   *
   * @param latitude 위도
   * @param longitude 경도
   */
  @Schema(description = "장소 좌표")
  public record Location(
      @Schema(
              description = "장소의 위도입니다.",
              example = "37.57000000",
              requiredMode = Schema.RequiredMode.REQUIRED)
          BigDecimal latitude,
      @Schema(
              description = "장소의 경도입니다.",
              example = "126.98500000",
              requiredMode = Schema.RequiredMode.REQUIRED)
          BigDecimal longitude) {}
}
