package com.todayit.common.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.todayit.common.file.entity.FileRelatedType;
import com.todayit.common.file.service.model.FileResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.tika.Tika;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.default_schema=\"todayit\"")
@DisplayName("실제 Cloudinary 파일 업로드")
class FileUploadIntegrationTest {
  @Autowired private FileManagementService fileManagementService;

  @Test
  @DisplayName("지정한 이미지 또는 PDF를 업로드하고 DB 조회 및 공개 URL 다운로드를 확인한다")
  void uploadsFileAndReadsItBack() throws IOException, InterruptedException {
    // 실제 업로드는 환경변수로 파일 경로를 지정했을 때만 실행한다.
    String filePath = System.getenv("FILE_UPLOAD_TEST_PATH");
    if (filePath == null || filePath.isBlank()) {
      filePath = System.getenv("FILE_UPLOAD_TEST_IMAGE");
    }
    assumeTrue(filePath != null && !filePath.isBlank(), "실제 업로드 파일 경로가 지정되지 않았습니다.");

    // given
    Path path = Path.of(filePath);
    byte[] content = Files.readAllBytes(path);
    String mimeType = new Tika().detect(content);
    MockMultipartFile attachment =
        new MockMultipartFile("files", path.getFileName().toString(), mimeType, content);
    String relatedId = UUID.randomUUID().toString();

    // when
    List<FileResult> uploaded =
        fileManagementService.upload(
            FileRelatedType.FAQ_ATTACHMENT, relatedId, List.of(attachment));

    // then
    assertThat(uploaded).hasSize(1);
    FileResult file = uploaded.getFirst();
    assertThat(file.sizeBytes()).isEqualTo(content.length);
    assertThat(file.mimeType()).isEqualTo(mimeType);
    assertThat(file.storedName())
        .startsWith("application/pdf".equals(mimeType) ? "pdf/" : "image/");
    assertThat(fileManagementService.findAll(FileRelatedType.FAQ_ATTACHMENT, relatedId))
        .extracting(FileResult::fileId)
        .containsExactly(file.fileId());
    String url =
        fileManagementService.getAccessUrl(
            FileRelatedType.FAQ_ATTACHMENT, relatedId, file.fileId());
    try (HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build()) {
      HttpResponse<byte[]> response =
          client.send(
              HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build(),
              HttpResponse.BodyHandlers.ofByteArray());
      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo(content);
    }
    System.out.println("UPLOAD_TEST_FILE_ID=" + file.fileId());
    System.out.println("UPLOAD_TEST_RELATED_ID=" + relatedId);
    System.out.println("UPLOAD_TEST_URL=" + url);
  }
}
