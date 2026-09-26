package com.todayit.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 회원에게 부여된 권한 정보를 나타냅니다. */
@Entity
@Table(name = "member_roles")
public class MemberRole {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "member_role_id")
  private Integer id;

  @Column(name = "member_id", nullable = false, length = 36)
  private String memberId;

  @Column(name = "roles_id", nullable = false)
  private Integer roleId;

  protected MemberRole() {}

  private MemberRole(String memberId, Integer roleId) {
    this.memberId = memberId;
    this.roleId = roleId;
  }

  /**
   * 회원에게 부여할 권한 정보를 생성합니다.
   *
   * @param memberId 회원 식별자
   * @param roleId 권한 식별자
   * @return 생성된 회원 권한 정보
   */
  public static MemberRole create(String memberId, Integer roleId) {
    return new MemberRole(memberId, roleId);
  }
}
