ALTER TABLE oauth_handshakes
    ADD COLUMN user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN return_path VARCHAR(1024);

CREATE INDEX idx_oauth_handshake_user ON oauth_handshakes(user_id);
