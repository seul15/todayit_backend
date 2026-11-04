package com.todayit.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.common.exception.BusinessException;
import com.todayit.common.exception.InvalidRequestException;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.service.FileManagementService;
import com.todayit.member.exception.MemberErrorCode;
import com.todayit.member.service.model.MemberProfileResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/** 프로필 이미지 업로드 흐름과 예외 처리를 테스트합니다. */
@ExtendWith(MockitoExtension.class)
class MemberProfileFacadeTest {
  private static final String MEMBER_ID = "member-123";
  private static final String PROFILE_IMAGE_URL = "https://example.com/profile.png";

  @Mock private ActiveMemberValidationService activeMemberValidationService;

  @Mock private MemberProfileService memberProfileService;

  @Mock private FileManagementService fileManagementService;

  private MemberProfileFacade facade;

  @BeforeEach
  void setUp() {
    facade =
        new MemberProfileFacade(
            activeMemberValidationService, memberProfileService, fileManagementService);
  }

  @Test
  @DisplayName("동의한 활성 회원은 프로필 이미지를 등록할 수 있다")
  void uploadsProfileImageForAgreedMember() {
    // Given
    MockMultipartFile file = createFile();

    MemberProfileResult expected =
        new MemberProfileResult(MEMBER_ID, PROFILE_IMAGE_URL, LocalDateTime.now());

    when(fileManagementService.uploadWithReferenceUpdate(
            eq(FileRelatedType.MEMBER_PROFILE), eq(MEMBER_ID), eq(List.of(file)), any()))
        .thenAnswer(
            invocation -> {
              Consumer<List<String>> callback = invocation.getArgument(3);
              callback.accept(List.of(PROFILE_IMAGE_URL));
              return List.of();
            });

    when(memberProfileService.changeProfileImage(MEMBER_ID, PROFILE_IMAGE_URL))
        .thenReturn(expected);

    // When
    MemberProfileResult result = facade.uploadProfileImage(MEMBER_ID, file);

    // Then
    assertThat(result).isEqualTo(expected);

    InOrder order =
        inOrder(activeMemberValidationService, memberProfileService, fileManagementService);

    order.verify(activeMemberValidationService).validateActiveMember(MEMBER_ID);
    order.verify(memberProfileService).validateProfileImageConsent(MEMBER_ID);

    order
        .verify(fileManagementService)
        .uploadWithReferenceUpdate(
            eq(FileRelatedType.MEMBER_PROFILE), eq(MEMBER_ID), eq(List.of(file)), any());

    order.verify(memberProfileService).changeProfileImage(MEMBER_ID, PROFILE_IMAGE_URL);
  }

  @Test
  @DisplayName("약관에 동의하지 않은 회원은 파일을 업로드할 수 없다")
  void rejectsMemberWithoutConsent() {
    // Given
    MockMultipartFile file = createFile();

    doThrow(new BusinessException(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED))
        .when(memberProfileService)
        .validateProfileImageConsent(MEMBER_ID);

    // When & Then
    assertThatThrownBy(() -> facade.uploadProfileImage(MEMBER_ID, file))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(MemberErrorCode.PROFILE_IMAGE_CONSENT_REQUIRED));

    verifyNoInteractions(fileManagementService);

    verify(memberProfileService, never()).changeProfileImage(anyString(), anyString());
  }

  @Test
  @DisplayName("비활성 회원은 프로필 이미지를 등록할 수 없다")
  void rejectsInactiveMember() {
    // Given
    MockMultipartFile file = createFile();

    doThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND))
        .when(activeMemberValidationService)
        .validateActiveMember(MEMBER_ID);

    // When & Then
    assertThatThrownBy(() -> facade.uploadProfileImage(MEMBER_ID, file))
        .isInstanceOf(BusinessException.class);

    verifyNoInteractions(memberProfileService, fileManagementService);
  }

  @Test
  @DisplayName("업로드 파일이 없으면 등록을 거부한다")
  void rejectsNullFile() {
    // When & Then
    assertThatThrownBy(() -> facade.uploadProfileImage(MEMBER_ID, null))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("프로필 이미지는 필수입니다.");

    verifyNoInteractions(
        activeMemberValidationService, memberProfileService, fileManagementService);
  }

  @Test
  @DisplayName("파일 업로드에 실패하면 회원 프로필을 변경하지 않는다")
  void doesNotUpdateMemberWhenUploadFails() {
    // Given
    MockMultipartFile file = createFile();

    FileException failure = new FileException(FileErrorCode.STORAGE_FAILED);

    when(fileManagementService.uploadWithReferenceUpdate(
            eq(FileRelatedType.MEMBER_PROFILE), eq(MEMBER_ID), eq(List.of(file)), any()))
        .thenThrow(failure);

    // When & Then
    assertThatThrownBy(() -> facade.uploadProfileImage(MEMBER_ID, file)).isSameAs(failure);

    verify(memberProfileService, never()).changeProfileImage(anyString(), anyString());
  }

  /** 테스트에서 사용할 이미지 파일을 생성합니다. */
  private MockMultipartFile createFile() {
    return new MockMultipartFile("file", "profile.png", "image/png", new byte[] {1, 2, 3});
  }
}
