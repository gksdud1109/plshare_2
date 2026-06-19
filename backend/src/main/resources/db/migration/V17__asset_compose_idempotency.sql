ALTER TABLE assets ADD COLUMN compose_idempotency_key VARCHAR(160);

ALTER TABLE assets
    ADD CONSTRAINT uk_assets_owner_compose_idempotency
    UNIQUE (owner_id, compose_idempotency_key);
