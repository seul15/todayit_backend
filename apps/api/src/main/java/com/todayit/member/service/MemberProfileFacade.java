package com.todayit.member.service;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileStorageException;
import com.todayit.common.file.service.FileManagementService;
import com.todayit.common.file.service.model.FileResult;
import com.todayit.member.service.model.MemberProfileResult;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 회원 프로필 이미지 등록 과정을 연결하는 Facade입니다. */
@Service
public class MemberProfileFacade {
  private static final Logger LOG = LoggerFactory.getLogger(MemberProfileFacade.class);

  private static final int PROFILE_IMAGE_LOCK_COUNT = 64;

  private final ReentrantLock[] profileImageLocks =
      IntStream.range(0, PROFILE_IMAGE_LOCK_COUNT)
          .mapToObj(index -> new ReentrantLock())
          .toArray(ReentrantLock[]::new);

  private final ActiveMemberValidationService activeMemberValidationService;
  private final MemberProfileService memberProfileService;
  private final FileManagementService fileManagementService;

  /**
   * 프로필 이미지 등록에 필요한 서비스를 주입받습니다.
   *
   * @param activeMemberValidationService 회원 상태 검증 서비스
   * @param memberProfileService 회원 프로필 변경 서비스
   * @param fileManagementService 공용 파일 관리 서비스
   */
  public MemberProfileFacade(
      ActiveMemberValidationService activeMemberValidationService,
      MemberProfileService memberProfileService,
      FileManagementService fileManagementService) {
    this.activeMemberValidationService = activeMemberValidationService;
    this.memberProfileService = memberProfileService;
    this.fileManagementService = fileManagementService;
  }

  /**
   * 회원의 프로필 이미지를 등록하거나 교체합니다.
   *
   * <p>파일 메타데이터 등록과 회원 프로필 이미지 변경을 동일한 DB 트랜잭션에서 처리합니다. 외부 파일 업로드는 DB 트랜잭션 밖에서 수행합니다.
   *
   * @param memberId 인증된 회원 식별자
   * @param file 업로드할 프로필 이미지
   * @return 변경된 회원 프로필 정보
   * @throws InvalidRequestException 업로드할 파일이 없는 경우
   */
  public MemberProfileResult uploadProfileImage(String memberId, MultipartFile file) {
    ReentrantLock lock = getProfileImageLock(memberId);
    lock.lock();

    try {
      if (file == null) {
        throw new InvalidRequestException("프로필 이미지는 필수입니다.");
      }

      activeMemberValidationService.validateActiveMember(memberId);

      memberProfileService.validateProfileImageConsent(memberId);

      AtomicReference<MemberProfileResult> profileResult = new AtomicReference<>();

      fileManagementService.uploadWithReferenceUpdate(
          FileRelatedType.MEMBER_PROFILE,
          memberId,
          List.of(file),
          urls -> {
            MemberProfileResult result =
                memberProfileService.changeProfileImage(memberId, urls.get(0));

            profileResult.set(result);
          });

      return profileResult.get();

    } finally {
      lock.unlock();
    }
  }

  /**
   * 회원의 프로필 이미지를 삭제하고 기본 이미지로 복원합니다.
   *
   * @param memberId 회원 식별자
   * @return 초기화된 프로필 이미지 정보
   */
  public MemberProfileResult deleteProfileImage(String memberId) {
    ReentrantLock lock = getProfileImageLock(memberId);
    lock.lock();

    try {
      activeMemberValidationService.validateActiveMember(memberId);

      List<Long> fileIds =
          fileManagementService.findAll(FileRelatedType.MEMBER_PROFILE, memberId).stream()
              .map(FileResult::fileId)
              .toList();

      MemberProfileResult result = memberProfileService.resetProfileImage(memberId);

      if (!fileIds.isEmpty()) {
        try {
          fileManagementService.delete(FileRelatedType.MEMBER_PROFILE, memberId, fileIds);
        } catch (FileStorageException exception) {
          LOG.warn("프로필 이미지 파일 정리 실패: memberId={}", memberId, exception);
          throw exception;
        }
      }

      return result;

    } finally {
      lock.unlock();
    }
  }

  private ReentrantLock getProfileImageLock(String memberId) {
    int index = Math.floorMod(memberId.hashCode(), PROFILE_IMAGE_LOCK_COUNT);
    return profileImageLocks[index];
  }
}
