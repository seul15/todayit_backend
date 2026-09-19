package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 로그인에 필요한 회원 정보를 나타냅니다. */
@Entity
@Table(name = "member")
public class Member {

  @Id
  @Column(name = "member_id", length = 36)
  private String id;

  @Column(name = "email", nullable = false, length = 100)
  private String email;

  @Column(name = "password", length = 100)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private MemberProvider provider;

  @Column(name = "is_active", nullable = false)
  private boolean active;

  protected Member() {}

  public String getId() {
    return id;
  }

  public String getPassword() {
    return password;
  }

  public boolean isActive() {
    return active;
  }
}
