ALTER TABLE member
    ADD COLUMN nickname_updated_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN member.nickname_updated_at
    IS '회원 닉네임 마지막 변경 시각';