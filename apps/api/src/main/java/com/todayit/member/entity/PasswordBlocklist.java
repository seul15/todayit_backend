package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 회원 비밀번호에 사용할 수 없는 금칙어 정보를 나타냅니다. */
@Entity
@Table(name = "password_blocklist")
public class PasswordBlocklist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "password_blocklist_id", nullable = false)
  private int passwordBlocklistId;

  @Column(name = "word", nullable = false, length = 100)
  private String word;

  @Enumerated(EnumType.STRING)
  @Column(name = "match_type", nullable = false, length = 20)
  private BlocklistMatchType matchType;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  protected PasswordBlocklist() {}
}
