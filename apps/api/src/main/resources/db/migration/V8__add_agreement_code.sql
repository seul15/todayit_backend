-- 약관 식별 코드 추가
ALTER TABLE agreement
    ADD COLUMN code VARCHAR(32);

-- 기존 약관 제목을 코드로 변환
UPDATE agreement
SET code = CASE title
               WHEN '서비스 이용약관' THEN 'TOS'
               WHEN '계정 관련 개인정보 수집·이용 동의' THEN 'ACCOUNT_PRIVACY'
               WHEN '선호정보 수집·이용 동의' THEN 'PREFERENCE'
               WHEN '프로필 이미지 수집·이용 동의' THEN 'PROFILE_IMAGE'
    END;

-- 변환되지 않은 약관이 있으면 마이그레이션 실패
ALTER TABLE agreement
    ALTER COLUMN code SET NOT NULL;

-- 동일한 약관 코드와 버전 중복 방지
ALTER TABLE agreement
    ADD CONSTRAINT uk_agreement_code_version
        UNIQUE (code, version);

-- 활성 약관은 코드별 하나만 허용
CREATE UNIQUE INDEX uk_agreement_active_code
    ON agreement (code)
    WHERE is_deleted = FALSE;
