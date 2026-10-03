INSERT INTO agreement (
    title,
    content,
    version,
    is_required
)
SELECT
    seed.title,
    seed.content,
    seed.version,
    seed.is_required
FROM (
         VALUES
             (
                 '서비스 이용약관',
                 '[로컬 개발용] 서비스 이용약관 테스트 데이터입니다.',
                 'v0.1',
                 TRUE
             ),
             (
                 '계정 관련 개인정보 수집·이용 동의',
                 '[로컬 개발용] 계정 생성 및 운영에 필요한 개인정보 수집·이용 테스트 데이터입니다.',
                 'v0.1',
                 FALSE
             ),
             (
                 '선호정보 수집·이용 동의',
                 '[로컬 개발용] 선호 지역 및 선호 테마 정보 수집·이용 테스트 데이터입니다.',
                 'v0.1',
                 FALSE
             ),
             (
                 '프로필 이미지 수집·이용 동의',
                 '[로컬 개발용] 프로필 이미지 수집·이용 테스트 데이터입니다.',
                 'v0.1',
                 FALSE
             )
     ) AS seed(title, content, version, is_required)
WHERE NOT EXISTS (
    SELECT 1
    FROM agreement
    WHERE agreement.title = seed.title
      AND agreement.version = seed.version
      AND agreement.is_deleted = FALSE
);