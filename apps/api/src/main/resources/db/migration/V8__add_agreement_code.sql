
-- 약관 식별 코드 추가 (기존 PK와 FK는 유지)
ALTER TABLE agreement
    ADD COLUMN code VARCHAR(32);

-- 기존 약관 데이터를 기획서의 코드로 변환
UPDATE agreement
SET code = CASE title
               WHEN '서비스 이용약관' THEN 'TOS'
               WHEN '계정 관련 개인정보 수집·이용 동의' THEN 'ACCOUNT_PRIVACY'
               WHEN '선호정보 수집·이용 동의' THEN 'PREFERENCE'
               WHEN '프로필 이미지 수집·이용 동의' THEN 'PROFILE_IMAGE'
    END
WHERE code IS NULL;

-- 변환되지 않은 약관이 있으면 마이그레이션 중단
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM agreement
        WHERE code IS NULL
    ) THEN
        RAISE EXCEPTION
            '기존 agreement 데이터 중 code가 매핑되지 않은 약관이 있습니다.';
END IF;
END $$;

ALTER TABLE agreement
    ALTER COLUMN code SET NOT NULL;

-- 같은 약관 코드에 여러 버전은 허용하되,
-- 동일한 코드와 버전의 중복은 금지
ALTER TABLE agreement
    ADD CONSTRAINT uk_agreement_code_version
        UNIQUE (code, version);

-- 현재 적용 중인 약관은 코드마다 하나만 허용
CREATE UNIQUE INDEX uk_agreement_active_code
    ON agreement (code)
    WHERE is_deleted = FALSE;

COMMENT ON COLUMN agreement.code IS
    '약관 식별 코드(TOS, ACCOUNT_PRIVACY, PREFERENCE, PROFILE_IMAGE)';
