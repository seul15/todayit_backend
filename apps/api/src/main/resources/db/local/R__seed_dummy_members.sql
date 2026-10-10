INSERT INTO region (si, gun, gu)
SELECT '서울특별시', '해당 없음', '종로구'
WHERE NOT EXISTS (
    SELECT 1
    FROM region
    WHERE si = '서울특별시'
      AND gun = '해당 없음'
      AND gu = '종로구'
);

INSERT INTO member (
    member_id,
    preferred_region,
    email,
    password,
    provider,
    nickname,
    profile_image,
    preferred_theme,
    is_active
)
SELECT
    seed.member_id,
    region.region_id,
    seed.email,
    seed.password,
    'LOCAL',
    seed.nickname,
    '/images/default-profile.png',
    NULL,
    TRUE
FROM (
    VALUES
        (
            '00000000-0000-0000-0000-000000000001',
            'user@todayit.local',
            '$2a$10$Yx34R9g2N8toLxLOseLVwePc.DqDjTwAgLpu6GelYT4rYR7sOvtPm',
            '더미사용자'
        ),
        (
            '00000000-0000-0000-0000-000000000002',
            'admin@todayit.local',
            '$2a$10$Yx34R9g2N8toLxLOseLVwePc.DqDjTwAgLpu6GelYT4rYR7sOvtPm',
            '더미관리자'
        ),
        (
            '00000000-0000-0000-0000-000000000003',
            'dev@todayit.local',
            '$2a$10$Yx34R9g2N8toLxLOseLVwePc.DqDjTwAgLpu6GelYT4rYR7sOvtPm',
            '더미개발자'
        )
) AS seed(member_id, email, password, nickname)
CROSS JOIN region
WHERE region.si = '서울특별시'
  AND region.gun = '해당 없음'
  AND region.gu = '종로구'
  AND NOT EXISTS (
      SELECT 1
      FROM member
      WHERE member.member_id = seed.member_id
         OR member.email = seed.email
  );

INSERT INTO member_roles (member_id, roles_id)
SELECT seed.member_id, roles.roles_id
FROM (
    VALUES
        ('00000000-0000-0000-0000-000000000001', 'USER'),
        ('00000000-0000-0000-0000-000000000002', 'ADMIN'),
        ('00000000-0000-0000-0000-000000000003', 'DEV')
) AS seed(member_id, role_name)
JOIN roles ON roles.name = seed.role_name
WHERE NOT EXISTS (
    SELECT 1
    FROM member_roles
    WHERE member_roles.member_id = seed.member_id
      AND member_roles.roles_id = roles.roles_id
);

UPDATE member
SET password = '$2a$10$Yx34R9g2N8toLxLOseLVwePc.DqDjTwAgLpu6GelYT4rYR7sOvtPm'
WHERE email IN (
    'user@todayit.local',
    'admin@todayit.local',
    'dev@todayit.local'
);
