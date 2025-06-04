CREATE TABLE IF NOT EXISTS study_material (
    id BIGSERIAL PRIMARY KEY,
    uploader_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject VARCHAR(100) NOT NULL,
    exam_type VARCHAR(50) NOT NULL
        CHECK ( exam_type IN ('중간고사', '기말고사') ),
    year INTEGER NOT NULL
        CHECK ( year >= 2018 AND year <= 2025 ),
    season VARCHAR(20) NOT NULL
        CHECK ( season IN ('1학기', '여름계절', '2학기', '겨울계절') ),
    title VARCHAR(200) NOT NULL,
    questions JSONB NOT NULL,
    question_count INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);