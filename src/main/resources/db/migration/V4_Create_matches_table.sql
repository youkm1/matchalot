
CREATE TABLE IF NOT EXISTS matches (
                                       id BIGSERIAL PRIMARY KEY,
                                       requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       requester_material_id BIGINT NOT NULL REFERENCES study_material(id) ON DELETE CASCADE,
                                       receiver_material_id BIGINT NOT NULL REFERENCES study_material(id) ON DELETE CASCADE,
                                       status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'COMPLETED', 'EXPIRED')),
                                       expired_at TIMESTAMP NOT NULL,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_matches_requester ON matches(requester_id);
CREATE INDEX idx_matches_partner ON matches(receiver_id);
CREATE INDEX idx_matches_status ON matches(status);
CREATE INDEX IF NOT EXISTS idx_matches_expires_at ON matches(expired_at);
CREATE INDEX IF NOT EXISTS idx_matches_requester_material ON matches(requester_material_id);
CREATE INDEX IF NOT EXISTS idx_matches_partner_material ON matches(receiver_material_id);