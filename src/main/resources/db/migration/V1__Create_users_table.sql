CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE
            CHECK ( email LIKE '%@sookmyung.ac.kr' ),
    nickname VARCHAR(100) NOT NULL,
    trust_score INTEGER NOT NULL DEFAULT 0
                                 CHECK ( trust_score >= -5 AND trust_score <= 5 ),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);