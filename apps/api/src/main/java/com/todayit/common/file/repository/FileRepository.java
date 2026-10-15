package com.todayit.common.file.repository;

import com.todayit.common.file.entity.File;
import com.todayit.common.file.entity.FileRelatedType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 파일 메타데이터 저장소입니다. */
public interface FileRepository extends JpaRepository<File, Long> {
  /**
   * 대상별 첨부를 식별자 순서로 조회합니다.
   *
   * @param relatedType 사용처
   * @param relatedId 대상 식별자
   * @return 첨부 목록
   */
  List<File> findByRelatedTypeAndRelatedIdOrderByFileIdAsc(
      FileRelatedType relatedType, String relatedId);

  /**
   * 동일 대상의 최초 업로드까지 직렬화하는 PostgreSQL 트랜잭션 잠금을 획득합니다.
   *
   * @param target 사용처와 식별자의 조합
   * @return 잠금 획득 결과
   */
  @Query(
      value = "SELECT 1 FROM pg_advisory_xact_lock(hashtextextended(:target, 0))",
      nativeQuery = true)
  Integer lockTarget(@Param("target") String target);
}
