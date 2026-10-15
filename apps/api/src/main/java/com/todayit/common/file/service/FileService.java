package com.todayit.common.file.service;

import com.todayit.common.file.client.FileStorage;
import com.todayit.common.file.entity.File;
import com.todayit.common.file.entity.File.FileSnapshot;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.exception.FileStorageException;
import com.todayit.common.file.service.model.FileRegistrationResult;
import com.todayit.common.file.service.model.FileResult;
import com.todayit.common.file.service.model.ValidatedFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 다른 기능에서 사용하는 공용 파일 서비스입니다. 호출자는 대상 존재 여부와 접근 권한을 먼저 검증해야 합니다. 업로드는 연결 대상 생성이 커밋된 후 DB 트랜잭션 밖에서
 * 호출합니다.
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class FileService implements FileManagementService {
  private static final Logger LOG = LoggerFactory.getLogger(FileService.class);
  private final FileValidationService validation;
  private final FileStorage storage;
  private final FileMetadataService metadata;

  /**
   * 공용 파일 서비스를 생성합니다.
   *
   * @param validation 파일 검증
   * @param storage 외부 저장소
   * @param metadata DB 저장
   */
  public FileService(
      FileValidationService validation, FileStorage storage, FileMetadataService metadata) {
    this.validation = validation;
    this.storage = storage;
    this.metadata = metadata;
  }

  /**
   * 파일을 업로드하고 대상에 연결합니다. 프로필 업로드는 이전 프로필을 교체합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자, 최대 36자
   * @param files 업로드할 파일
   * @return 저장된 메타데이터
   * @throws FileException 대상 또는 파일 입력이 유효하지 않은 경우
   */
  @Override
  public List<FileResult> upload(
      FileRelatedType type, String relatedId, List<MultipartFile> files) {
    validateTarget(type, relatedId);
    List<ValidatedFile> validated = validation.validate(type, files);
    List<File> attempted = new ArrayList<>();
    boolean registered = false;
    try {
      for (ValidatedFile file : validated) {
        boolean pdf = "application/pdf".equals(file.extension().mimeType());
        String storedName = (pdf ? "pdf/" : "image/") + UUID.randomUUID();
        if (pdf) {
          storedName += ".pdf";
        }
        byte[] content = file.content();
        File stored =
            new File(
                type,
                relatedId,
                file.originalName(),
                storedName,
                file.extension().mimeType(),
                content.length);
        attempted.add(stored);
        storage.upload(storedName, file.extension().mimeType(), type, content);
      }
      FileRegistrationResult result = metadata.register(type, relatedId, attempted);
      registered = true;
      for (FileResult previous : result.replaced()) {
        deleteUnusedFile(previous.storedName(), previous.mimeType(), previous.relatedType());
      }
      return result.files();
    } finally {
      if (!registered) {
        compensate(attempted);
      }
    }
  }

  /**
   * 대상 파일 목록을 반환합니다. 접근 권한은 호출자가 검증해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @return 파일 목록
   * @throws FileException 대상 입력이 유효하지 않은 경우
   */
  @Override
  public List<FileResult> findAll(FileRelatedType type, String relatedId) {
    validateTarget(type, relatedId);
    return metadata.findAll(type, relatedId);
  }

  /**
   * 파일 접근 URL을 반환합니다. 문의 첨부의 URL은 호출자가 본인 문의 또는 관리자 권한을 검증한 뒤 발급해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @param fileId 파일 식별자
   * @return 접근 URL
   * @throws FileException 대상 또는 파일이 유효하지 않은 경우
   */
  @Override
  public String getAccessUrl(FileRelatedType type, String relatedId, long fileId) {
    validateTarget(type, relatedId);
    FileResult file = metadata.get(type, relatedId, fileId);
    return storage.accessUrl(file.storedName(), file.mimeType(), type);
  }

  /**
   * 대상에 속한 외부 파일을 먼저 삭제한 뒤 성공한 파일의 메타데이터를 제거합니다. 중간 실패 시에도 이전에 삭제한 파일은 정리하고 원래 예외를 전파합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @param fileIds 삭제할 파일 식별자
   * @throws FileException 대상 또는 식별자 입력이 유효하지 않은 경우
   */
  @Override
  public void delete(FileRelatedType type, String relatedId, List<Long> fileIds) {
    validateTarget(type, relatedId);
    if (fileIds == null || fileIds.stream().anyMatch(id -> id == null || id < 1)) {
      throw new FileException(FileErrorCode.INVALID_FILE_IDS);
    }
    List<FileResult> selected =
        metadata.findAll(type, relatedId).stream()
            .filter(file -> fileIds.contains(file.fileId()))
            .toList();
    if (selected.size() != fileIds.stream().distinct().count()) {
      throw new FileException(FileErrorCode.NOT_FOUND);
    }
    List<Long> deletedIds = new ArrayList<>();
    try {
      for (FileResult file : selected) {
        storage.delete(file.storedName(), file.mimeType(), file.relatedType());
        deletedIds.add(file.fileId());
      }
    } catch (RuntimeException failure) {
      if (!deletedIds.isEmpty()) {
        try {
          metadata.delete(type, relatedId, deletedIds);
        } catch (RuntimeException cleanupFailure) {
          failure.addSuppressed(cleanupFailure);
        }
      }
      throw failure;
    }
    if (!deletedIds.isEmpty()) {
      metadata.delete(type, relatedId, deletedIds);
    }
  }

  /**
   * 파일 사용처와 연결 대상 식별자의 필수 값, 길이 및 제어 문자를 검사합니다.
   *
   * @param type 파일 사용처
   * @param relatedId 연결 대상 식별자
   * @throws FileException 사용처가 없거나 식별자가 비어 있거나 36자를 초과하거나 제어 문자를 포함할 때
   */
  private void validateTarget(FileRelatedType type, String relatedId) {
    if (type == null
        || relatedId == null
        || relatedId.isBlank()
        || relatedId.length() > 36
        || relatedId.codePoints().anyMatch(Character::isISOControl)) {
      throw new FileException(FileErrorCode.INVALID_TARGET);
    }
  }

  /**
   * 메타데이터 저장에 실패한 업로드 파일을 즉시 삭제합니다. 정리 실패로 원래 예외를 덮어쓰지 않습니다.
   *
   * @param attempted 업로드를 시도한 파일 목록
   */
  private void compensate(List<File> attempted) {
    for (File file : attempted) {
      FileSnapshot snapshot = file.getSnapshot();
      deleteUnusedFile(snapshot.storedName(), snapshot.mimeType(), snapshot.relatedType());
    }
  }

  /**
   * 더 이상 사용하지 않는 외부 파일을 삭제하고, 실패하면 수동 정리에 필요한 저장 파일명을 기록합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 파일 MIME 유형
   * @param type 파일 사용처
   */
  private void deleteUnusedFile(String storedName, String mimeType, FileRelatedType type) {
    try {
      storage.delete(storedName, mimeType, type);
    } catch (FileStorageException exception) {
      LOG.error("미사용 외부 파일 삭제 실패: storedName={}", storedName);
    }
  }
}
