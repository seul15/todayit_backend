package com.todayit.common.file.service;

import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.service.model.FileResult;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 다른 기능에서 사용하는 파일 업로드, 조회와 삭제 계약입니다. 호출자는 대상 존재 여부와 접근 권한을 검증하고 DB 트랜잭션 밖에서 호출해야 합니다. 업로드 대상은 생성
 * 트랜잭션이 커밋된 후 사용합니다.
 */
public interface FileManagementService {

  /**
   * 파일을 업로드하고 대상에 연결합니다. 프로필 업로드는 이전 프로필을 교체합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자, 최대 36자
   * @param files 업로드할 파일
   * @return 저장된 메타데이터
   * @throws FileException 대상 또는 파일 입력이 유효하지 않은 경우
   */
  List<FileResult> upload(FileRelatedType type, String relatedId, List<MultipartFile> files);

  /**
   * 대상 파일 목록을 반환합니다. 접근 권한은 호출자가 검증해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @return 파일 목록
   * @throws FileException 대상 입력이 유효하지 않은 경우
   */
  List<FileResult> findAll(FileRelatedType type, String relatedId);

  /**
   * 파일 접근 URL을 반환합니다. 문의 첨부의 URL은 호출자가 본인 문의 또는 관리자 권한을 검증한 뒤 발급해야 합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @param fileId 파일 식별자
   * @return 접근 URL
   * @throws FileException 대상 또는 파일이 유효하지 않은 경우
   */
  String getAccessUrl(FileRelatedType type, String relatedId, long fileId);

  /**
   * 대상에 속한 외부 파일을 먼저 삭제한 뒤 성공한 파일의 메타데이터를 제거합니다. 중간 실패 시에도 이전에 삭제한 파일은 정리하고 원래 예외를 전파합니다.
   *
   * @param type 사용처
   * @param relatedId 대상 식별자
   * @param fileIds 삭제할 파일 식별자
   * @throws FileException 대상 또는 식별자 입력이 유효하지 않은 경우
   */
  void delete(FileRelatedType type, String relatedId, List<Long> fileIds);
}
