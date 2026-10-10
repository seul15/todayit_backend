INSERT INTO course (
    member_id,
    title,
    course_summary,
    transport,
    start_at,
    end_at,
    status,
    visibility,
    view_count
)
SELECT
    seed.member_id,
    seed.title,
    seed.course_summary,
    seed.transport,
    seed.start_at,
    seed.end_at,
    seed.status,
    seed.visibility,
    seed.view_count
FROM (
    VALUES
        (
            '00000000-0000-0000-0000-000000000001',
            '종로 맛집과 카페 코스',
            '종로에서 식사와 카페를 함께 즐기는 코스입니다.',
            'WALKING',
            TIMESTAMP WITH TIME ZONE '2026-10-03 11:00:00+09:00',
            TIMESTAMP WITH TIME ZONE '2026-10-03 15:00:00+09:00',
            'ACTIVE',
            'PUBLIC',
            25
        ),
        (
            '00000000-0000-0000-0000-000000000001',
            '종로 문화 산책 코스',
            '전시와 산책을 함께 즐기는 문화 코스입니다.',
            'WALKING',
            TIMESTAMP WITH TIME ZONE '2026-10-04 13:00:00+09:00',
            TIMESTAMP WITH TIME ZONE '2026-10-04 17:00:00+09:00',
            'ACTIVE',
            'PUBLIC',
            18
        )
) AS seed(
    member_id,
    title,
    course_summary,
    transport,
    start_at,
    end_at,
    status,
    visibility,
    view_count
)
WHERE EXISTS (
    SELECT 1
    FROM member
    WHERE member.member_id = seed.member_id
)
AND NOT EXISTS (
    SELECT 1
    FROM course
    WHERE course.title = seed.title
);

INSERT INTO course_place (
    course_id,
    place_id,
    "order",
    stay_minute
)
SELECT
    course.course_id,
    place.place_id,
    seed.place_order,
    seed.stay_minute
FROM (
    VALUES
        ('종로 맛집과 카페 코스', '오늘의 식당', 0, 90),
        ('종로 맛집과 카페 코스', '종로 카페거리', 1, 60),
        ('종로 맛집과 카페 코스', '인사동 소품 상점', 2, 45),
        ('종로 문화 산책 코스', '서울 전시 공간', 0, 90),
        ('종로 문화 산책 코스', '북악산 산책로', 1, 120),
        ('종로 문화 산책 코스', '남산 전망대', 2, 60)
) AS seed(course_title, place_name, place_order, stay_minute)
JOIN course ON course.title = seed.course_title
JOIN place ON place.name = seed.place_name
WHERE NOT EXISTS (
    SELECT 1
    FROM course_place
    WHERE course_place.course_id = course.course_id
      AND course_place."order" = seed.place_order
);
