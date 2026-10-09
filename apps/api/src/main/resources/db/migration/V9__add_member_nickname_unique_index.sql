
-- 영문 대소문자와 앞뒤 공백을 제외하고 닉네임 중복 방지
CREATE UNIQUE INDEX uk_member_nickname_normalized
    ON member (LOWER(BTRIM(nickname)));

COMMENT ON INDEX uk_member_nickname_normalized IS
    '닉네임 중복 방지: 영문 대소문자 무시 및 앞뒤 공백 제외';
