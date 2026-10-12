-- 약관 버전 길이 확장
ALTER TABLE agreement
    ALTER COLUMN version SET DATA TYPE VARCHAR(8);

-- 선호 지역 선택 사항으로 변경
ALTER TABLE member
    ALTER COLUMN preferred_region DROP NOT NULL;

-- 소셜 로그인 계정을 고려해 이메일 NULL 허용
ALTER TABLE member
    ALTER COLUMN email DROP NOT NULL;

-- 소셜 로그인 사용자 식별자 추가
ALTER TABLE member
    ADD COLUMN provider_user_id VARCHAR(255);

-- 이메일 중복 기준을 가입 방식까지 확장
ALTER TABLE member
DROP CONSTRAINT uk_member_email;

ALTER TABLE member
    ADD CONSTRAINT uk_member_email_provider
        UNIQUE (email, provider);