package com.todayit.member.repository;

import com.todayit.member.entity.MemberAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

/** 회원의 약관 동의 정보를 저장하는 JPA Repository입니다. */
public interface MemberAgreementRepository extends JpaRepository<MemberAgreement, Integer> {}
