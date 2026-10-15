package com.todayit.common.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.todayit.common.file.client.FileStorage;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.exception.FileStorageException;
import com.todayit.common.file.service.model.FileRegistrationResult;
import com.todayit.common.file.service.model.FileResult;
import com.todayit.common.file.service.model.ValidatedFile;
import com.todayit.common.file.validation.FileExtension;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.dao.DataAccessResourceFailureException;

@DisplayName("공용 파일 서비스")
class FileServiceTest {
  private final FileValidationService validation = mock(FileValidationService.class);
  private final FileStorage storage = mock(FileStorage.class);
  private final FileMetadataService metadata = mock(FileMetadataService.class);
  private final FileManagementService service = new FileService(validation, storage, metadata);

  @Test
  @DisplayName("성공 여부가 불명확한 업로드도 즉시 삭제하고 원래 오류를 전파한다")
  void deletesEvenTheAttemptWithUnknownUploadOutcome() {
    // given
    when(validation.validate(eq(FileRelatedType.INQUIRY_ATTACHMENT), anyList()))
        .thenReturn(List.of(new ValidatedFile("a.pdf", FileExtension.PDF, new byte[] {1})));
    FileStorageException failure = new FileStorageException();
    doThrow(failure).when(storage).upload(anyString(), anyString(), any(), any());

    // when
    Throwable thrown =
        catchThrowable(() -> service.upload(FileRelatedType.INQUIRY_ATTACHMENT, "23", List.of()));

    // then
    assertThat(thrown).isSameAs(failure);
    ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
    verify(storage)
        .delete(
            storedName.capture(), eq("application/pdf"), eq(FileRelatedType.INQUIRY_ATTACHMENT));
    assertThat(storedName.getValue()).startsWith("pdf/").endsWith(".pdf");
    verifyNoInteractions(metadata);
  }

  @Test
  @DisplayName("메타데이터 저장 실패 시 업로드한 파일을 즉시 삭제한다")
  void deletesUploadedFilesWhenMetadataCannotCommit() {
    // given
    when(validation.validate(eq(FileRelatedType.FAQ_ATTACHMENT), anyList()))
        .thenReturn(List.of(new ValidatedFile("a.png", FileExtension.PNG, new byte[] {1})));
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("db failure");
    when(metadata.register(eq(FileRelatedType.FAQ_ATTACHMENT), eq("23"), anyList()))
        .thenThrow(failure);

    // when
    Throwable thrown =
        catchThrowable(() -> service.upload(FileRelatedType.FAQ_ATTACHMENT, "23", List.of()));

    // then
    assertThat(thrown).isSameAs(failure);
    ArgumentCaptor<String> storedName = ArgumentCaptor.forClass(String.class);
    verify(storage)
        .delete(storedName.capture(), eq("image/png"), eq(FileRelatedType.FAQ_ATTACHMENT));
    assertThat(storedName.getValue()).startsWith("image/");
    verify(storage)
        .upload(
            eq(storedName.getValue()), eq("image/png"), eq(FileRelatedType.FAQ_ATTACHMENT), any());
  }

  @Test
  @DisplayName("여러 업로드 파일 중 정리 실패가 있어도 나머지를 정리하고 원래 오류를 유지한다")
  void preservesOriginalFailureAndContinuesCleanup() {
    // given
    when(validation.validate(eq(FileRelatedType.FAQ_ATTACHMENT), anyList()))
        .thenReturn(
            List.of(
                new ValidatedFile("a.png", FileExtension.PNG, new byte[] {1}),
                new ValidatedFile("b.png", FileExtension.PNG, new byte[] {2})));
    DataAccessResourceFailureException failure =
        new DataAccessResourceFailureException("db failure");
    when(metadata.register(eq(FileRelatedType.FAQ_ATTACHMENT), eq("23"), anyList()))
        .thenThrow(failure);
    doThrow(new FileStorageException()).when(storage).delete(anyString(), anyString(), any());

    // when
    Throwable thrown =
        catchThrowable(() -> service.upload(FileRelatedType.FAQ_ATTACHMENT, "23", List.of()));

    // then
    assertThat(thrown).isSameAs(failure);
    verify(storage, org.mockito.Mockito.times(2))
        .delete(anyString(), eq("image/png"), eq(FileRelatedType.FAQ_ATTACHMENT));
  }

  @Test
  @DisplayName("프로필 교체가 커밋된 다음 이전 외부 파일을 삭제한다")
  void deletesPreviousProfileAfterRegistration() {
    // given
    when(validation.validate(eq(FileRelatedType.MEMBER_PROFILE), anyList()))
        .thenReturn(List.of(new ValidatedFile("a.png", FileExtension.PNG, new byte[] {1})));
    FileResult previous = file(1, FileRelatedType.MEMBER_PROFILE, "old-key");
    FileResult added = file(2, FileRelatedType.MEMBER_PROFILE, "new-key");
    when(metadata.register(eq(FileRelatedType.MEMBER_PROFILE), eq("23"), anyList()))
        .thenReturn(new FileRegistrationResult(List.of(added), List.of(previous)));

    // when
    List<FileResult> result = service.upload(FileRelatedType.MEMBER_PROFILE, "23", List.of());

    // then
    assertThat(result).containsExactly(added);
    InOrder order = inOrder(metadata, storage);
    order.verify(metadata).register(eq(FileRelatedType.MEMBER_PROFILE), eq("23"), anyList());
    order.verify(storage).delete("old-key", "image/png", FileRelatedType.MEMBER_PROFILE);
  }

  @Test
  @DisplayName("이전 프로필 삭제가 실패해도 커밋된 새 프로필을 삭제하지 않는다")
  void keepsNewProfileWhenPreviousCleanupFails() {
    // given
    when(validation.validate(eq(FileRelatedType.MEMBER_PROFILE), anyList()))
        .thenReturn(List.of(new ValidatedFile("a.png", FileExtension.PNG, new byte[] {1})));
    FileResult added = file(2, FileRelatedType.MEMBER_PROFILE, "new-key");
    when(metadata.register(eq(FileRelatedType.MEMBER_PROFILE), eq("23"), anyList()))
        .thenReturn(
            new FileRegistrationResult(
                List.of(added), List.of(file(1, FileRelatedType.MEMBER_PROFILE, "old-key"))));
    doThrow(new FileStorageException())
        .when(storage)
        .delete("old-key", "image/png", FileRelatedType.MEMBER_PROFILE);

    // when
    List<FileResult> result = service.upload(FileRelatedType.MEMBER_PROFILE, "23", List.of());

    // then
    assertThat(result).containsExactly(added);
    verify(storage, never()).delete(eq("new-key"), anyString(), any());
  }

  @Test
  @DisplayName("외부 파일 삭제가 성공한 뒤 메타데이터를 삭제한다")
  void deletesStorageBeforeMetadata() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(List.of(file(1, FileRelatedType.FAQ_ATTACHMENT, "key")));

    // when
    service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L));

    // then
    InOrder order = inOrder(storage, metadata);
    order.verify(storage).delete("key", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    order.verify(metadata).delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L));
  }

  @Test
  @DisplayName("외부 삭제가 실패하면 메타데이터를 유지하여 삭제를 다시 요청할 수 있다")
  void keepsMetadataWhenStorageDeletionFails() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(List.of(file(1, FileRelatedType.FAQ_ATTACHMENT, "key")));
    FileStorageException failure = new FileStorageException();
    doThrow(failure).when(storage).delete("key", "image/png", FileRelatedType.FAQ_ATTACHMENT);

    // when
    Throwable thrown =
        catchThrowable(() -> service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L)));

    // then
    assertThat(thrown).isSameAs(failure);
    verify(metadata, never()).delete(any(), anyString(), anyList());
  }

  @Test
  @DisplayName("여러 파일 중간에 외부 삭제가 실패하면 성공한 파일의 메타데이터만 정리한다")
  void removesSuccessfulMetadataWhenLaterStorageDeletionFails() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(
            List.of(
                file(1, FileRelatedType.FAQ_ATTACHMENT, "first"),
                file(2, FileRelatedType.FAQ_ATTACHMENT, "second"),
                file(3, FileRelatedType.FAQ_ATTACHMENT, "third")));
    FileStorageException failure = new FileStorageException();
    doThrow(failure).when(storage).delete("second", "image/png", FileRelatedType.FAQ_ATTACHMENT);

    // when
    Throwable thrown =
        catchThrowable(
            () -> service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L, 3L)));

    // then
    assertThat(thrown).isSameAs(failure);
    InOrder order = inOrder(storage, metadata);
    order.verify(storage).delete("first", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    order.verify(storage).delete("second", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    order.verify(metadata).delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L));
    verify(storage, never()).delete("third", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    verify(metadata, never()).delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L, 3L));
  }

  @Test
  @DisplayName("부분 삭제 후 메타데이터 정리까지 실패해도 원래 외부 삭제 예외를 유지한다")
  void preservesStorageFailureWhenPartialMetadataCleanupFails() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(
            List.of(
                file(1, FileRelatedType.FAQ_ATTACHMENT, "first"),
                file(2, FileRelatedType.FAQ_ATTACHMENT, "second")));
    FileStorageException failure = new FileStorageException();
    DataAccessResourceFailureException cleanupFailure =
        new DataAccessResourceFailureException("db failure");
    doThrow(failure).when(storage).delete("second", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    doThrow(cleanupFailure)
        .when(metadata)
        .delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L));

    // when
    Throwable thrown =
        catchThrowable(() -> service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L)));

    // then
    assertThat(thrown).isSameAs(failure);
    assertThat(thrown.getSuppressed()).containsExactly(cleanupFailure);
  }

  @Test
  @DisplayName("모든 외부 삭제가 성공하면 중복 없는 성공 식별자 전체의 메타데이터를 정리한다")
  void removesAllSuccessfulMetadataOnce() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(
            List.of(
                file(1, FileRelatedType.FAQ_ATTACHMENT, "first"),
                file(2, FileRelatedType.FAQ_ATTACHMENT, "second")));

    // when
    service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L, 1L));

    // then
    InOrder order = inOrder(storage, metadata);
    order.verify(storage).delete("first", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    order.verify(storage).delete("second", "image/png", FileRelatedType.FAQ_ATTACHMENT);
    order.verify(metadata).delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L));
  }

  @Test
  @DisplayName("다른 대상의 파일을 포함한 삭제 요청은 외부 삭제 전에 거부한다")
  void rejectsMissingFileBeforeDeletingStorage() {
    // given
    when(metadata.findAll(FileRelatedType.FAQ_ATTACHMENT, "23"))
        .thenReturn(List.of(file(1, FileRelatedType.FAQ_ATTACHMENT, "key")));

    // when
    Throwable thrown =
        catchThrowable(() -> service.delete(FileRelatedType.FAQ_ATTACHMENT, "23", List.of(1L, 2L)));

    // then
    assertThat(thrown)
        .isInstanceOfSatisfying(
            FileException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.NOT_FOUND));
    verifyNoInteractions(storage);
    verify(metadata, never()).delete(any(), anyString(), anyList());
  }

  @Test
  @DisplayName("잘못된 연결 대상은 외부 작업 전에 커스텀 오류로 거부한다")
  void rejectsInvalidTargetBeforeAnyExternalOperation() {
    // given
    String invalidTarget = " ";

    // when
    Throwable thrown =
        catchThrowable(
            () -> service.upload(FileRelatedType.MEMBER_PROFILE, invalidTarget, List.of()));

    // then
    assertThat(thrown)
        .isInstanceOfSatisfying(
            FileException.class,
            exception ->
                assertThat(exception.getErrorCode()).isEqualTo(FileErrorCode.INVALID_TARGET));
    verifyNoInteractions(storage, metadata, validation);
  }

  private FileResult file(long id, FileRelatedType type, String storedName) {
    return new FileResult(
        id,
        type,
        "23",
        "photo.png",
        storedName,
        "image/png",
        1,
        LocalDateTime.of(2026, 1, 1, 0, 0));
  }
}
