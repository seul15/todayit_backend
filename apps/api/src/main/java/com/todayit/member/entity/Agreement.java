package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 서비스에서 제공하는 약관 정보를 나타냅니다. */
@Entity
@Table(name = "agreement")
public class Agreement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "agreement_id")
  private Integer id;

  @Column(name = "code", nullable = false, length = 32)
  private String code;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "content", nullable = false, length = 8000)
  private String content;

  @Column(name = "version", nullable = false, length = 8)
  private String version;

  @Column(name = "is_required", nullable = false)
  private boolean required;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  protected Agreement() {}

  public Integer getId() {
    return id;
  }

  public String getCode() {
    return code;
  }

  public boolean isRequired() {
    return required;
  }
}
