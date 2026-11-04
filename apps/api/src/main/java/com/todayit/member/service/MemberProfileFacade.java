package com.todayit.member.service;

import com.todayit.common.exception.InvalidRequestException;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.service.FileManagementService;
import com.todayit.common.file.service.model.FileResult;
import com.todayit.member.service.model.MemberProfileResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 회원 프로필 이미지 등록 과정을 연결하는 Facade입니다. */
@Service
public class MemberProfileFacade {
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
   * <p>공용 파일 서비스가 트랜잭션 밖에서 실행되도록 Facade에는 트랜잭션을 적용하지 않습니다.
   *
   * @param memberId 인증된 회원 식별자
   * @param file 업로드할 프로필 이미지
   * @return 변경된 회원 프로필 정보
   * @throws InvalidRequestException 업로드할 파일이 없는 경우
   */
  public MemberProfileResult uploadProfileImage(String memberId, MultipartFile file) {

    // 프로필 이미지 파일 필수 확인
    if (file == null) {
      throw new InvalidRequestException("프로필 이미지는 필수입니다.");
    }

    // 회원 존재 여부 및 활성 상태 검증
    activeMemberValidationService.validateActiveMember(memberId);

    // 프로필 이미지 약관 동의 여부 검증
    memberProfileService.validateProfileImageConsent(memberId);

    // Cloudinary 업로드 및 파일 메타데이터 저장
    List<FileResult> uploadedFiles =
        fileManagementService.upload(FileRelatedType.MEMBER_PROFILE, memberId, List.of(file));

    // 업로드된 이미지의 접근 URL 조회
    FileResult uploadedFile = uploadedFiles.get(0);

    String profileImageUrl =
        fileManagementService.getAccessUrl(
            FileRelatedType.MEMBER_PROFILE, memberId, uploadedFile.fileId());

    // 회원 프로필 이미지 URL 변경
    return memberProfileService.changeProfileImage(memberId, profileImageUrl);
  }
}
