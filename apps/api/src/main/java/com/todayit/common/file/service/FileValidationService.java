package com.todayit.common.file.service;

import com.todayit.common.file.config.FileUploadProperties;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileErrorCode;
import com.todayit.common.file.exception.FileException;
import com.todayit.common.file.service.model.ValidatedFile;
import com.todayit.common.file.validation.FileExtension;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.tika.Tika;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 파일 크기, 확장자, 선언된 MIME과 실제 바이트의 MIME을 함께 검증합니다. */
@Service
public class FileValidationService {
  private final int maxFileBytes;
  private final FileUploadProperties properties;
  private final Tika tika = new Tika();

  /**
   * HTTP 업로드 설정과 문의 첨부 설정을 받아 검증 제한을 구성합니다.
   *
   * @param multipartProperties HTTP multipart 설정
   * @param properties 문의 첨부 제한 설정
   * @throws IllegalArgumentException 파일 크기 제한이 양수가 아니거나 메모리에 읽을 수 있는 범위를 초과할 때
   */
  public FileValidationService(
      MultipartProperties multipartProperties, FileUploadProperties properties) {
    long configuredBytes = multipartProperties.getMaxFileSize().toBytes();
    if (configuredBytes < 1 || configuredBytes >= Integer.MAX_VALUE) {
      throw new IllegalArgumentException("파일 크기 제한은 1바이트 이상이며 2GiB보다 작아야 합니다.");
    }
    this.maxFileBytes = (int) configuredBytes;
    this.properties = properties;
  }

  /**
   * 외부 업로드 전에 전체 입력을 검증합니다. 탐지는 악성 코드 검사나 완전한 파일 파싱이 아닙니다.
   *
   * @param type 사용처
   * @param files 업로드할 파일
   * @return 검증한 파일 목록
   * @throws FileException 크기 또는 형식이 잘못되거나 파일을 읽을 수 없는 경우
   */
  public List<ValidatedFile> validate(FileRelatedType type, List<MultipartFile> files) {
    if (type == null
        || files == null
        || files.isEmpty()
        || files.stream().anyMatch(f -> f == null)) {
      throw new FileException(FileErrorCode.INVALID_FILES);
    }
    if (type == FileRelatedType.MEMBER_PROFILE && files.size() != 1) {
      throw new FileException(FileErrorCode.PROFILE_LIMIT_EXCEEDED);
    }
    if (type == FileRelatedType.INQUIRY_ATTACHMENT && files.size() > properties.inquiryMaxFiles()) {
      throw new FileException(FileErrorCode.INQUIRY_LIMIT_EXCEEDED);
    }
    List<ValidatedFile> validated = files.stream().map(file -> validateOne(type, file)).toList();
    if (type == FileRelatedType.INQUIRY_ATTACHMENT
        && validated.stream().mapToLong(ValidatedFile::sizeBytes).sum()
            > properties.inquiryMaxTotalSize().toBytes()) {
      throw new FileException(FileErrorCode.INQUIRY_LIMIT_EXCEEDED);
    }
    return validated;
  }

  /**
   * 파일명, 크기, 확장자와 MIME 유형을 검사하고 실제 내용의 유형까지 확인합니다.
   *
   * @param type 파일 사용처
   * @param file 검증할 업로드 파일
   * @return 검증된 메타데이터와 파일 내용
   * @throws FileException 파일 제약을 위반하거나 내용을 읽을 수 없을 때
   */
  private ValidatedFile validateOne(FileRelatedType type, MultipartFile file) {
    String name = file.getOriginalFilename();
    if (name == null
        || name.isBlank()
        || name.length() > 255
        || name.contains("/")
        || name.contains("\\")
        || name.codePoints().anyMatch(Character::isISOControl)) {
      throw new FileException(FileErrorCode.INVALID_NAME);
    }
    if (file.getSize() < 1 || file.getSize() > maxFileBytes) {
      throw new FileException(FileErrorCode.INVALID_SIZE);
    }
    int dot = name.lastIndexOf('.');
    if (dot < 1) {
      throw new FileException(FileErrorCode.UNSUPPORTED_EXTENSION);
    }
    String suffix = name.substring(dot + 1);
    FileExtension extension =
        Arrays.stream(FileExtension.values())
            .filter(candidate -> candidate.value().equalsIgnoreCase(suffix))
            .findFirst()
            .orElseThrow(() -> new FileException(FileErrorCode.UNSUPPORTED_EXTENSION));
    if (type == FileRelatedType.MEMBER_PROFILE && extension == FileExtension.PDF) {
      throw new FileException(FileErrorCode.PROFILE_IMAGE_REQUIRED);
    }
    if (!extension.mimeType().equalsIgnoreCase(file.getContentType())) {
      throw new FileException(FileErrorCode.MIME_MISMATCH);
    }
    byte[] content = readContent(file);
    if (content.length < 1 || content.length > maxFileBytes || content.length != file.getSize()) {
      throw new FileException(FileErrorCode.INVALID_SIZE);
    }
    // 파일명을 탐지기에 전달하지 않아 확장자가 바이트 탐지를 보완하지 못하도록 한다.
    if (!extension.mimeType().equals(tika.detect(content))) {
      throw new FileException(FileErrorCode.CONTENT_MISMATCH);
    }
    return new ValidatedFile(name, extension, content);
  }

  /**
   * 최대 허용 크기보다 한 바이트 더 읽어 초과 여부를 검사할 수 있도록 파일 내용을 가져옵니다.
   *
   * @param file 내용을 읽을 파일
   * @return 최대 허용 크기보다 한 바이트까지 읽은 내용
   * @throws FileException 파일 내용을 읽지 못할 때
   */
  private byte[] readContent(MultipartFile file) {
    // MultipartFile 스트림의 checked IOException만 파일 예외로 변환한다.
    try (InputStream input = file.getInputStream()) {
      return input.readNBytes(maxFileBytes + 1);
    } catch (IOException exception) {
      throw new FileException(FileErrorCode.READ_FAILED);
    }
  }
}
