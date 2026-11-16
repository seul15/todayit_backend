package com.todayit.course.service;

import com.todayit.course.entity.Region;
import com.todayit.course.entity.Region.RegionSnapshot;
import com.todayit.course.exception.InvalidRegionPathException;
import com.todayit.course.repository.RegionRepository;
import com.todayit.course.service.model.RegionFilter;
import com.todayit.course.service.model.RegionResult;
import com.todayit.course.service.model.SigunguResult;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 행정 지역 코드를 검증하고 조회하는 Query Service입니다. */
@Service
public class RegionQueryService implements RegionPlaceQueryService {

  private final RegionRepository regionRepository;

  /**
   * 지역 조회에 필요한 Repository를 받습니다.
   *
   * @param regionRepository 지역 Repository
   */
  public RegionQueryService(RegionRepository regionRepository) {
    this.regionRepository = regionRepository;
  }

  /**
   * 전체 행정 지역을 시·도 및 하위 시·군·구 계층 구조로 조회합니다.
   *
   * @return 시·도별 시·군·구 계층 구조 목록
   */
  @Transactional(readOnly = true)
  public List<RegionResult> findAllRegions() {
    List<RegionSnapshot> snapshots =
        regionRepository.findAllByOrderBySidoCodeAscSigunguCodeAsc().stream()
            .map(Region::getSnapshot)
            .toList();

    Map<String, List<RegionSnapshot>> groupedBySido =
        snapshots.stream()
            .collect(
                Collectors.groupingBy(
                    RegionSnapshot::sidoCode, LinkedHashMap::new, Collectors.toList()));

    return groupedBySido.values().stream()
        .map(
            regionList -> {
              RegionSnapshot first = regionList.get(0);
              List<SigunguResult> sigungus =
                  regionList.stream()
                      .map(r -> new SigunguResult(r.sigunguCode(), r.sigunguName()))
                      .toList();
              return new RegionResult(first.sidoCode(), first.sidoName(), sigungus);
            })
        .toList();
  }

  /**
   * 전달한 지역 코드가 모두 일치하는 지역 식별자를 조회합니다.
   *
   * @param sidoCode 시·도 코드 2자리, 생략 가능
   * @param sigunguCode 시·군·구 코드 3자리, 생략 가능
   * @return 조건에 해당하는 지역 식별자 목록
   * @throws InvalidRegionPathException 코드 형식이 잘못되었거나 해당 지역이 없을 때
   */
  @Transactional(readOnly = true)
  @Override
  public List<Integer> findRegionIds(String sidoCode, String sigunguCode) {
    if ((sidoCode != null && !sidoCode.matches("[0-9]{2}"))
        || (sigunguCode != null && !sigunguCode.matches("[0-9]{3}"))) {
      throw new InvalidRegionPathException();
    }
    List<Integer> regionIds = regionRepository.findIdsByCodes(sidoCode, sigunguCode);
    if (regionIds.isEmpty()) {
      throw new InvalidRegionPathException();
    }
    return List.copyOf(regionIds);
  }

  /**
   * 전달한 지역 필터 조건에 해당하는 지역 식별자 목록을 조회합니다.
   *
   * @param filters 지역 필터 목록
   * @return 조건에 해당하는 지역 식별자 목록
   * @throws InvalidRegionPathException 코드 형식이 잘못되었거나 해당 지역이 없을 때
   */
  @Transactional(readOnly = true)
  @Override
  public List<Integer> findRegionIds(List<RegionFilter> filters) {
    if (filters == null || filters.isEmpty()) {
      return List.of();
    }

    Set<Integer> allRegionIds = new LinkedHashSet<>();
    for (RegionFilter filter : filters) {
      String sidoCode = filter.sidoCode();
      if (sidoCode == null || !sidoCode.matches("[0-9]{2}")) {
        throw new InvalidRegionPathException();
      }

      List<String> sigunguCodes = filter.sigunguCodes();
      if (sigunguCodes == null || sigunguCodes.isEmpty()) {
        List<Integer> ids = regionRepository.findIdsBySidoCode(sidoCode);
        if (ids.isEmpty()) {
          throw new InvalidRegionPathException();
        }
        allRegionIds.addAll(ids);
      } else {
        for (String sigunguCode : sigunguCodes) {
          if (sigunguCode == null || !sigunguCode.matches("[0-9]{3}")) {
            throw new InvalidRegionPathException();
          }
        }
        List<String> distinctSigunguCodes = sigunguCodes.stream().distinct().toList();
        List<Region> matchedRegions =
            regionRepository.findBySidoCodeAndSigunguCodeIn(sidoCode, distinctSigunguCodes);

        Set<String> matchedSigunguCodes =
            matchedRegions.stream()
                .map(r -> r.getSnapshot().sigunguCode())
                .collect(Collectors.toSet());

        if (matchedSigunguCodes.size() != distinctSigunguCodes.size()) {
          throw new InvalidRegionPathException();
        }

        matchedRegions.forEach(r -> allRegionIds.add(r.getSnapshot().regionId()));
      }
    }

    return List.copyOf(allRegionIds);
  }
}
