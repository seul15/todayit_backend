
-- 회원가입 및 소셜 로그인 지원을 위한 기존 스키마 확장
-- 기존 V1 마이그레이션은 수정하지 않고 신규 버전에서 변경사항 적용

-- 약관 버전 길이 확장
ALTER TABLE agreement
ALTER COLUMN version TYPE VARCHAR(8);

-- 회원가입 시 선호 지역을 필수로 입력하지 않도록 변경
ALTER TABLE member
    ALTER COLUMN preferred_region DROP NOT NULL;

-- 소셜 로그인 계정을 고려하여 이메일 nullable 허용
ALTER TABLE member
    ALTER COLUMN email DROP NOT NULL;

-- 소셜 로그인 제공자 사용자 식별자 추가
ALTER TABLE member
    ADD COLUMN provider_user_id VARCHAR(255);

-- 이메일 단독 UNIQUE를 이메일 + 가입 방식 UNIQUE로 변경
ALTER TABLE member
DROP CONSTRAINT uk_member_email;

ALTER TABLE member
    ADD CONSTRAINT uk_member_email_provider
        UNIQUE (email, provider);

COMMENT ON COLUMN member.provider_user_id IS
    '소셜 로그인 제공자 사용자 식별자';
