-- 영문 대소문자와 앞뒤 공백을 제외한 닉네임 중복 방지
CREATE UNIQUE INDEX uk_member_nickname_normalized
    ON member (LOWER(BTRIM(nickname)));
