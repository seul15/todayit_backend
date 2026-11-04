package com.todayit.common.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.todayit.common.file.entity.File;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.repository.FileRepository;
import com.todayit.member.entity.Member;
import com.todayit.member.repository.MemberRepository;
import com.todayit.member.service.MemberProfileService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest
@DisplayName("파일 등록 동시성")
class FileConcurrencyTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:17-alpine")
          .withDatabaseName("todayit")
          .withUsername("todayit")
          .withPassword("test-password")
          .withUrlParam("currentSchema", "todayit");

  @Autowired private FileMetadataService metadata;
  @Autowired private FileRepository fileRepository;
  @Autowired private MemberRepository memberRepository;
  @Autowired private MemberProfileService memberProfileService;

  @Test
  @DisplayName("같은 문의에 최초 업로드 6개가 동시에 요청되어도 5개까지만 저장한다")
  void concurrentFirstUploadsCannotExceedFiveInquiryAttachments() throws Exception {
    try (var executor = Executors.newFixedThreadPool(6)) {
      // given
      CountDownLatch start = new CountDownLatch(1);
      List<Future<Throwable>> attempts = new ArrayList<>();
      for (int index = 0; index < 6; index++) {
        int number = index;
        attempts.add(
            executor.submit(
                () -> {
                  start.await();
                  return catchThrowable(
                      () ->
                          metadata.register(
                              FileRelatedType.INQUIRY_ATTACHMENT,
                              "concurrent-inquiry",
                              List.of(
                                  new File(
                                      FileRelatedType.INQUIRY_ATTACHMENT,
                                      "concurrent-inquiry",
                                      "photo.png",
                                      "concurrent-" + number,
                                      "image/png",
                                      1))));
                }));
      }
      // when
      start.countDown();
      List<Throwable> results = new ArrayList<>();
      for (Future<Throwable> attempt : attempts) {
        results.add(attempt.get());
      }
      var attached = metadata.findAll(FileRelatedType.INQUIRY_ATTACHMENT, "concurrent-inquiry");

      // then
      assertThat(results).filteredOn(result -> result == null).hasSize(5);
      assertThat(results)
          .filteredOn(result -> result != null)
          .singleElement()
          .isInstanceOfSatisfying(
              FileException.class,
              exception ->
                  assertThat(exception.getErrorCode())
                      .isEqualTo(FileErrorCode.INQUIRY_LIMIT_EXCEEDED));
      assertThat(attached).hasSize(5);
    }
  }

  @Test
  @DisplayName("회원정보 저장 실패 시 파일 메타데이터 변경도 롤백한다")
  void rollsBackFileReplacementWhenMemberUpdateFails() {
    // Given
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String oldUrl = "https://example.com/old.png";
    String oldStoredName = "image/old-" + suffix;
    String memberId;

    Member member =
        Member.createLocal(
            "rollback-" + suffix + "@example.com", "encoded-password", "rollback" + suffix);

    member.changeProfileImage(oldUrl, LocalDateTime.now());
    memberRepository.saveAndFlush(member);
    memberId = member.getId();

    File oldFile =
        fileRepository.saveAndFlush(
            new File(
                FileRelatedType.MEMBER_PROFILE,
                memberId,
                "old.png",
                oldStoredName,
                "image/png",
                100));

    File newFile =
        new File(
            FileRelatedType.MEMBER_PROFILE,
            memberId,
            "new.png",
            "image/new-" + suffix,
            "image/png",
            100);

    // profile_image 컬럼의 최대 길이(2048자)를 초과
    String invalidUrl = "https://example.com/" + "a".repeat(2048);

    AtomicBoolean callbackExecuted = new AtomicBoolean(false);

    try {
      // When: 실제 DB 저장 실패 발생
      Throwable thrown =
          catchThrowable(
              () ->
                  metadata.registerWithCallback(
                      FileRelatedType.MEMBER_PROFILE,
                      memberId,
                      List.of(newFile),
                      saved -> {
                        memberProfileService.changeProfileImage(memberId, invalidUrl);

                        callbackExecuted.set(true);
                      }));

      // Then: 회원정보 변경 작업까지 실행됐는지 확인
      assertThat(callbackExecuted.get()).isTrue();
      assertThat(thrown).isInstanceOf(RuntimeException.class);

      // 회원 프로필 이미지가 이전 값으로 유지되어야 함
      Member restored = memberRepository.findById(memberId).orElseThrow();

      assertThat(restored.getProfileImage()).isEqualTo(oldUrl);

      // 파일 메타데이터도 이전 파일로 복구되어야 함
      var files = metadata.findAll(FileRelatedType.MEMBER_PROFILE, memberId);

      assertThat(files)
          .singleElement()
          .satisfies(
              file -> {
                assertThat(file.fileId()).isEqualTo(oldFile.getSnapshot().fileId());
                assertThat(file.storedName()).isEqualTo(oldStoredName);
              });

    } finally {
      // 테스트에서 생성한 데이터 정리
      fileRepository.deleteAll(
          fileRepository.findByRelatedTypeAndRelatedIdOrderByFileIdAsc(
              FileRelatedType.MEMBER_PROFILE, memberId));

      memberRepository.deleteById(memberId);
    }
  }
}
