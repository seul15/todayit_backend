ALTER TABLE place DROP CONSTRAINT ck_place_category;

ALTER TABLE place ADD CONSTRAINT ck_place_category CHECK (
    category IN (
        'RESTAURANT',
        'CAFE_DESSERT',
        'EXHIBITION_MUSEUM',
        'PERFORMANCE',
        'EXPERIENCE',
        'PLAY_LEISURE',
        'NATURE_WALK',
        'SHOPPING',
        'LANDMARK_VIEW'
    )
);
