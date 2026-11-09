ALTER TABLE member
    ALTER COLUMN profile_image
        SET DEFAULT 'https://res.cloudinary.com/xmb52jrp/image/upload/v1793191339/default_profile.png';

UPDATE member
SET profile_image = 'https://res.cloudinary.com/xmb52jrp/image/upload/v1793191339/default_profile.png'
WHERE profile_image = '/images/default-profile.png';

ALTER TABLE member
    ADD COLUMN nickname_updated_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN member.nickname_updated_at
    IS '회원 닉네임 마지막 변경 시각';

ALTER TABLE member
ALTER COLUMN preferred_theme TYPE VARCHAR(20)[]
    USING CASE
        WHEN preferred_theme IS NULL THEN NULL
        ELSE ARRAY[preferred_theme]
END;

COMMENT ON COLUMN member.preferred_theme
    IS '회원 선호 테마 코드 목록';

ALTER TABLE member
DROP CONSTRAINT fk_member_preferred_region;

ALTER TABLE member
ALTER COLUMN preferred_region TYPE VARCHAR(20)[]
    USING CASE
        WHEN preferred_region IS NULL THEN NULL
        ELSE ARRAY[preferred_region::VARCHAR(20)]
END;

ALTER TABLE member
    ADD CONSTRAINT ck_member_preferred_region_count
        CHECK (
            preferred_region IS NULL
                OR cardinality(preferred_region) <= 3
            );

COMMENT ON COLUMN member.preferred_region
    IS '회원 선호 지역 식별값 목록, 최대 3개';
