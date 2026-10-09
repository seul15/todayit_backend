package com.todayit.member.repository;

import com.todayit.member.entity.NicknameBlocklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 닉네임 금칙어 정보를 조회하는 JPA Repository입니다. */
public interface NicknameBlocklistRepository extends JpaRepository<NicknameBlocklist, Integer> {

  /**
   * 입력 닉네임이 활성화된 금칙어 정책에 해당하는지 확인합니다.
   *
   * @param nickname 확인할 닉네임
   * @return 금칙어에 해당하면 true
   */
  @Query(
      value =
          """
                  SELECT EXISTS (
                      SELECT 1
                      FROM nickname_blocklist
                      WHERE enabled = TRUE
                        AND (
                            (match_type = 'EXACT'
                             AND LOWER(word) = LOWER(:nickname))
                            OR
                            (match_type = 'CONTAINS'
                             AND POSITION(LOWER(word) IN LOWER(:nickname)) > 0)
                        )
                  )
                  """,
      nativeQuery = true)
  boolean existsBlockedNickname(@Param("nickname") String nickname);
}
