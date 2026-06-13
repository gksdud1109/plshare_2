ALTER TABLE assets
    ADD COLUMN owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE import_jobs
    ADD COLUMN owner_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE import_jobs
    ADD COLUMN spotify_grant_id UUID REFERENCES spotify_access_grants(id) ON DELETE SET NULL;
ALTER TABLE export_jobs
    ADD COLUMN owner_id UUID REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_assets_owner_id ON assets(owner_id);
CREATE INDEX idx_import_jobs_owner_id ON import_jobs(owner_id);
CREATE INDEX idx_export_jobs_owner_id ON export_jobs(owner_id);
