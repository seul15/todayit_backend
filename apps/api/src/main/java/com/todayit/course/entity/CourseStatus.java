package com.todayit.course.entity;

/** 코스의 상태입니다. */
public enum CourseStatus {
  /** 임시 저장 상태입니다. */
  DRAFT,

  /** 공개 가능한 활성 상태입니다. */
  ACTIVE,

  /** 삭제된 상태입니다. */
  DELETED
}
