package com.todayit.common.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 파일 저장소 설정을 활성화합니다. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({CloudinaryProperties.class, FileUploadProperties.class})
public class FileConfig {}
