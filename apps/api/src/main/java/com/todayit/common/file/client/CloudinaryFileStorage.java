package com.todayit.common.file.client;

import com.todayit.common.file.config.CloudinaryProperties;
import com.todayit.common.file.dto.response.CloudinaryDeleteResponse;
import com.todayit.common.file.dto.response.CloudinaryUploadResponse;
import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.exception.FileStorageException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/** Cloudinary 저장소 구현입니다. 비밀값 및 제공자 오류 응답은 로그와 예외에 포함하지 않습니다. */
@Component
public class CloudinaryFileStorage implements FileStorage {
  private final CloudinaryProperties properties;
  private final WebClient client;

  /**
   * Cloudinary 클라이언트를 생성합니다. 인증 정보는 실제 작업 시 검증합니다.
   *
   * @param properties 환경별 설정
   */
  @Autowired
  public CloudinaryFileStorage(CloudinaryProperties properties) {
    this(properties, WebClient.builder().baseUrl("https://api.cloudinary.com").build());
  }

  CloudinaryFileStorage(CloudinaryProperties properties, WebClient client) {
    this.properties = properties;
    this.client = client;
  }

  /**
   * Cloudinary에 파일을 업로드하고 응답의 파일명, 유형과 크기가 요청과 일치하는지 확인합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME 유형
   * @param type 파일 사용처
   * @param content 업로드할 파일 내용
   * @throws FileStorageException 설정이 유효하지 않거나 업로드 또는 응답 검증에 실패할 때
   */
  @Override
  public void upload(String storedName, String mimeType, FileRelatedType type, byte[] content) {
    checkConfiguration();
    MultipartBodyBuilder body = new MultipartBodyBuilder();
    body.part(
            "file",
            new ByteArrayResource(content) {
              /**
               * 멀티파트 업로드에 사용할 파일명을 반환합니다.
               *
               * @return 업로드용 파일명
               */
              @Override
              public String getFilename() {
                return "upload";
              }
            })
        .contentType(MediaType.parseMediaType(mimeType));
    body.part("public_id", storedName);
    body.part("asset_folder", "application/pdf".equals(mimeType) ? "pdf" : "image");
    body.part("type", type.deliveryType());
    body.part("overwrite", "false");
    CloudinaryUploadResponse response =
        await(
            client
                .post()
                .uri(endpoint(mimeType, "upload"))
                .headers(
                    headers -> headers.setBasicAuth(properties.apiKey(), properties.apiSecret()))
                .body(BodyInserters.fromMultipartData(body.build()))
                .retrieve()
                .bodyToMono(CloudinaryUploadResponse.class));
    if (response == null
        || !storedName.equals(response.publicId())
        || !resourceType(mimeType).equals(response.resourceType())
        || !type.deliveryType().equals(response.type())
        || response.bytes() != content.length) {
      throw new FileStorageException();
    }
  }

  /**
   * Cloudinary 파일을 삭제하고 CDN 캐시 무효화를 요청합니다. 이미 없는 파일도 삭제 성공으로 처리합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME 유형
   * @param type 파일 사용처
   * @throws FileStorageException 설정이 유효하지 않거나 삭제에 실패할 때
   */
  @Override
  public void delete(String storedName, String mimeType, FileRelatedType type) {
    checkConfiguration();
    LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("public_id", storedName);
    body.add("type", type.deliveryType());
    body.add("invalidate", "true");
    CloudinaryDeleteResponse response =
        await(
            client
                .post()
                .uri(endpoint(mimeType, "destroy"))
                .headers(
                    headers -> headers.setBasicAuth(properties.apiKey(), properties.apiSecret()))
                .body(BodyInserters.fromFormData(body))
                .retrieve()
                .bodyToMono(CloudinaryDeleteResponse.class));
    if (response == null
        || !("ok".equals(response.result()) || "not found".equals(response.result()))) {
      throw new FileStorageException();
    }
  }

  /**
   * 일반 파일에는 공개 URL을, 문의 첨부에는 만료 시간을 지정하지 않은 서명 URL을 생성합니다.
   *
   * @param storedName 저장 파일명
   * @param mimeType 검증된 MIME 유형
   * @param type 파일 사용처
   * @return 파일 접근 URL
   * @throws FileStorageException 설정이 유효하지 않거나 서명 생성에 실패할 때
   */
  @Override
  public String accessUrl(String storedName, String mimeType, FileRelatedType type) {
    checkConfiguration();
    if (type != FileRelatedType.INQUIRY_ATTACHMENT) {
      return UriComponentsBuilder.fromUriString("https://res.cloudinary.com")
          .pathSegment(properties.cloudName(), resourceType(mimeType), "upload", "v1")
          .path("/" + storedName)
          .build()
          .encode()
          .toUriString();
    }
    String signature = sign(storedName);
    return UriComponentsBuilder.fromUriString("https://res.cloudinary.com")
        .pathSegment(
            properties.cloudName(),
            resourceType(mimeType),
            type.deliveryType(),
            "s--" + signature + "--")
        .path("/" + storedName)
        .build()
        .encode()
        .toUriString();
  }

  /**
   * 클라우드 이름과 파일 리소스 유형에 맞는 Cloudinary API 경로를 구성합니다.
   *
   * @param mimeType 파일 MIME 유형
   * @param action 실행할 API 작업
   * @return Cloudinary API 경로
   */
  private String endpoint(String mimeType, String action) {
    return "/v1_1/" + properties.cloudName() + "/" + resourceType(mimeType) + "/" + action;
  }

  /**
   * PDF는 raw 리소스로, 나머지는 이미지 리소스로 분류합니다.
   *
   * @param mimeType 검증된 MIME 유형
   * @return Cloudinary 리소스 유형
   */
  private String resourceType(String mimeType) {
    return "application/pdf".equals(mimeType) ? "raw" : "image";
  }

  /**
   * 설정된 제한 시간 동안 응답을 기다리고 통신, 디코딩 및 시간 초과 오류를 저장소 예외로 변환합니다.
   *
   * @param <T> 응답 본문 유형
   * @param response 외부 저장소 응답
   * @return 응답 본문. 본문 없이 완료되면 null
   * @throws FileStorageException 통신, 디코딩, 버퍼 제한 또는 시간 초과 오류가 발생할 때
   */
  private <T> T await(Mono<T> response) {
    return response
        .timeout(properties.timeout())
        .onErrorMap(WebClientException.class, exception -> new FileStorageException())
        .onErrorMap(CodecException.class, exception -> new FileStorageException())
        .onErrorMap(DataBufferLimitException.class, exception -> new FileStorageException())
        .onErrorMap(TimeoutException.class, exception -> new FileStorageException())
        .block();
  }

  /**
   * 전달할 파일 경로와 API 비밀키로 Cloudinary 전달 URL 서명을 생성합니다.
   *
   * @param path 서명 뒤에 이어질 파일 경로
   * @return URL 안전 Base64 해시의 앞 8자리
   * @throws FileStorageException SHA-1 알고리즘을 사용할 수 없을 때
   */
  private String sign(String path) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-1")
              .digest((path + properties.apiSecret()).getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 8);
    } catch (NoSuchAlgorithmException exception) {
      throw new FileStorageException();
    }
  }

  /**
   * Cloudinary 인증 설정과 요청 제한 시간이 사용 가능한지 검사합니다.
   *
   * @throws FileStorageException 필수 설정이 없거나 형식 및 시간 값이 유효하지 않을 때
   */
  private void checkConfiguration() {
    if (properties.cloudName() == null
        || !properties.cloudName().matches("[a-zA-Z0-9_-]+")
        || properties.apiKey() == null
        || properties.apiKey().isBlank()
        || properties.apiSecret() == null
        || properties.apiSecret().isBlank()
        || properties.timeout() == null
        || properties.timeout().isNegative()
        || properties.timeout().isZero()) {
      throw new FileStorageException();
    }
  }
}
