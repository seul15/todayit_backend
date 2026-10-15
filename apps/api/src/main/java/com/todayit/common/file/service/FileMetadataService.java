package com.todayit.common.file.service;

import com.todayit.common.file.config.FileUploadProperties;
import com.todayit.common.file.entity.File;
import com.todayit.common.file.entity.File.FileSnapshot;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.repository.FileRepository;
import com.todayit.common.file.service.model.FileRegistrationResult;
import com.todayit.common.file.service.model.FileResult;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 외부 호출 없이 파일 메타데이터를 저장하고 조회합니다. */
@Service
public class FileMetadataService {
  private final FileRepository files;
  private final FileUploadProperties properties;

  /**
   * 메타데이터 서비스를 생성합니다.
   *
   * @param files 파일 저장소
   * @param properties 문의 첨부 제한 설정
   */
  public FileMetadataService(FileRepository files, FileUploadProperties properties) {
    this.files = files;
    this.properties = properties;
  }

  /**
   * 검증 및 업로드가 끝난 파일을 연결합니다. 프로필은 교체하고 문의는 누적 제한을 검증합니다.
   *
   * @param type 사용처
   * @param relatedId 연결 대상
   * @param additions 업로드 완료 파일
   * @return 저장한 파일과 교체된 이전 프로필 파일
   * @throws FileException 누적 문의 첨부 제한을 초과하는 경우
   */
  @Transactional
  public FileRegistrationResult register(
      FileRelatedType type, String relatedId, List<File> additions) {
    files.lockTarget(type.name() + ":" + relatedId);
    List<File> existing = files.findByRelatedTypeAndRelatedIdOrderByFileIdAsc(type, relatedId);
    if (type == FileRelatedType.INQUIRY_ATTACHMENT) {
      long total =
          existing.stream().map(File::getSnapshot).mapToLong(FileSnapshot::sizeBytes).sum()
              + additions.stream().map(File::getSnapshot).mapToLong(FileSnapshot::sizeBytes).sum();
      if (existing.size() + additions.size() > properties.inquiryMaxFiles()
          || total > properties.inquiryMaxTotalSize().toBytes()) {
        throw new FileException(FileErrorCode.INQUIRY_LIMIT_EXCEEDED);
      }
    }
    List<FileResult> replaced = List.of();
    if (type == FileRelatedType.MEMBER_PROFILE) {
      replaced = existing.stream().map(File::getSnapshot).map(FileResult::from).toList();
      files.deleteAll(existing);
      files.flush();
    }
    List<FileResult> saved =
        files.saveAllAndFlush(additions).stream()
            .map(File::getSnapshot)
            .map(FileResult::from)
            .toList();
    return new FileRegistrationResult(saved, replaced);
  }

  /**
   * 대상의 파일을 조회합니다. 대상 접근 권한은 호출자가 검증해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 연결 대상
   * @return 파일 목록
   */
  @Transactional(readOnly = true)
  public List<FileResult> findAll(FileRelatedType type, String relatedId) {
    return files.findByRelatedTypeAndRelatedIdOrderByFileIdAsc(type, relatedId).stream()
        .map(File::getSnapshot)
        .map(FileResult::from)
        .toList();
  }

  /**
   * 지정한 대상에 속한 파일을 조회합니다.
   *
   * @param type 사용처
   * @param relatedId 연결 대상
   * @param fileId 파일 식별자
   * @return 파일 메타데이터
   * @throws FileException 대상에 속한 파일이 없는 경우
   */
  @Transactional(readOnly = true)
  public FileResult get(FileRelatedType type, String relatedId, long fileId) {
    return files
        .findById(fileId)
        .filter(
            file ->
                file.getSnapshot().relatedType() == type
                    && file.getSnapshot().relatedId().equals(relatedId))
        .map(File::getSnapshot)
        .map(FileResult::from)
        .orElseThrow(() -> new FileException(FileErrorCode.NOT_FOUND));
  }

  /**
   * 외부 저장소에서 삭제한 파일의 메타데이터를 제거합니다. 대상 권한은 호출자가 검증해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 연결 대상
   * @param fileIds 삭제할 파일 식별자, 빈 목록이면 변경 없음
   * @throws FileException 요청한 파일이 해당 대상에 속하지 않는 경우
   */
  @Transactional
  public void delete(FileRelatedType type, String relatedId, List<Long> fileIds) {
    files.lockTarget(type.name() + ":" + relatedId);
    List<File> selected =
        files.findByRelatedTypeAndRelatedIdOrderByFileIdAsc(type, relatedId).stream()
            .filter(file -> fileIds.contains(file.getSnapshot().fileId()))
            .toList();
    if (selected.size() != fileIds.stream().distinct().count()) {
      throw new FileException(FileErrorCode.NOT_FOUND);
    }
    files.deleteAll(selected);
  }
}
