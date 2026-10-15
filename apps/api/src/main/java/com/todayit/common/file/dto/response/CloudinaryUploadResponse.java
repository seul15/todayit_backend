package com.todayit.common.file.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 업로드한 파일의 식별 정보와 크기를 담은 Cloudinary 응답입니다.
 *
 * @param publicId 저장 파일명
 * @param resourceType 리소스 유형
 * @param type 전달 유형
 * @param bytes 저장된 파일 크기(바이트)
 */
public record CloudinaryUploadResponse(
    @JsonProperty("public_id") String publicId,
    @JsonProperty("resource_type") String resourceType,
    String type,
    long bytes) {}
