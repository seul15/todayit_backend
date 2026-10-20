-- 같은 소셜 제공자에서 발급한 동일 사용자 ID의 중복 가입 방지
-- LOCAL 계정은 provider_user_id가 NULL이므로 인덱스에서 제외
CREATE UNIQUE INDEX uk_member_provider_user_id
    ON member (provider, provider_user_id)
    WHERE provider_user_id IS NOT NULL;
