ALTER TABLE place ADD COLUMN description TEXT;
COMMENT ON COLUMN place.description IS '장소 설명';
