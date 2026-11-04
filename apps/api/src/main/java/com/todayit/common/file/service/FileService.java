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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
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
   * 파일 업로드와 해당 파일을 참조하는 DB 정보 변경을 함께 처리합니다.
   *
   * <p>외부 저장소 업로드와 접근 URL 생성은 DB 트랜잭션 밖에서 수행합니다. 파일 메타데이터와 후속 DB 정보 변경이 모두 성공한 뒤 이전 외부 파일을 정리합니다.
   *
   * <p>DB 변경에 실패하면 새로 업로드한 파일의 삭제를 시도합니다. 후속 작업은 별도의 트랜잭션을 시작하지 않아야 합니다.
   *
   * @param type 파일 사용처
   * @param relatedId 연결 대상 식별자, 최대 36자
   * @param files 업로드할 파일 목록
   * @param updateReferences 등록된 파일 URL을 전달받아 DB 정보를 변경하는 작업
   * @return 등록된 파일 메타데이터
   * @throws FileException 대상 또는 파일 입력이 유효하지 않은 경우
   * @throws IllegalArgumentException 후속 DB 변경 작업이 없는 경우
   * @throws RuntimeException 외부 저장소 또는 후속 DB 작업에 실패한 경우
   */
  @Override
  public List<FileResult> uploadWithReferenceUpdate(
      FileRelatedType type,
      String relatedId,
      List<MultipartFile> files,
      Consumer<List<String>> updateReferences) {

    if (updateReferences == null) {
      throw new IllegalArgumentException("후속 DB 변경 작업은 필수입니다.");
    }

    validateTarget(type, relatedId);

    List<ValidatedFile> validated = validation.validate(type, files);
    List<File> attempted = new ArrayList<>();
    boolean registered = false;

    try {
      // 1. 기존 방식으로 파일을 검증된 외부 저장소에 업로드
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

      // 2. DB 트랜잭션에 들어가기 전에 접근 URL 생성
      Map<String, String> uploadedUrls = new HashMap<>();

      for (File file : attempted) {
        FileSnapshot snapshot = file.getSnapshot();

        String url =
            storage.accessUrl(snapshot.storedName(), snapshot.mimeType(), snapshot.relatedType());

        uploadedUrls.put(snapshot.storedName(), url);
      }

      // 3. 파일 메타데이터와 참조 DB 정보를 동일한 트랜잭션에서 갱신
      FileRegistrationResult result =
          metadata.registerWithCallback(
              type,
              relatedId,
              attempted,
              saved -> {
                List<String> urls =
                    saved.stream()
                        .map(
                            file -> {
                              String url = uploadedUrls.get(file.storedName());

                              if (url == null) {
                                throw new IllegalStateException("등록된 파일의 접근 URL을 찾을 수 없습니다.");
                              }

                              return url;
                            })
                        .toList();

                updateReferences.accept(urls);
              });

      registered = true;

      // 4. DB 커밋 성공 후 이전 외부 파일 정리
      for (FileResult previous : result.replaced()) {
        deleteUnusedFile(previous.storedName(), previous.mimeType(), previous.relatedType());
      }

      return result.files();

    } finally {
      // 5. DB 등록 실패 시 새로 업로드한 외부 파일 정리
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
