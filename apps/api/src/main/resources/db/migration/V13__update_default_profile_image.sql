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