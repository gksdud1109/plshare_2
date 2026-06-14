-- 선물 메시지 상한 500 → 3000자(편지 길이)로 확대.
-- 데모(H2 ddl-auto)는 엔티티가 스키마 소스이므로 이 마이그레이션은 prod(Postgres/Flyway) 전용.
ALTER TABLE gifts ALTER COLUMN message TYPE VARCHAR(3000);
