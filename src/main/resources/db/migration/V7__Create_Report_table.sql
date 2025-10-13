-- V7__Create_reports_table.sql
-- 신고 기능을 위한 reports 테이블 생성

CREATE TABLE IF NOT EXISTS reports (
                                       id BIGSERIAL PRIMARY KEY,
                                       reporter_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       reported_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       material_id BIGINT REFERENCES study_material(id) ON DELETE SET NULL,
                                       report_type VARCHAR(50) NOT NULL
                                           CHECK (report_type IN ('INAPPROPRIATE_CONTENT', 'FAKE_MATERIAL', 'COPYRIGHT_VIOLATION', 'SPAM', 'HARASSMENT', 'OTHER')),
                                       description TEXT,
                                       status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                           CHECK (status IN ('PENDING', 'RESOLVED', 'REJECTED')),
                                       admin_note TEXT,
                                       resolved_at TIMESTAMP,
                                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성 (조회 성능 향상)
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_reported_user ON reports(reported_user_id);
CREATE INDEX idx_reports_material ON reports(material_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_type ON reports(report_type);
CREATE INDEX idx_reports_created_at ON reports(created_at);

-- 복합 인덱스 (중복 신고 확인용)
CREATE UNIQUE INDEX idx_reports_reporter_material ON reports(reporter_id, material_id)
    WHERE material_id IS NOT NULL;
CREATE UNIQUE INDEX idx_reports_reporter_user ON reports(reporter_id, reported_user_id)
    WHERE material_id IS NULL;

-- 상태별 조회 성능 향상
CREATE INDEX idx_reports_status_created ON reports(status, created_at);

-- 신고자별 조회 성능 향상
CREATE INDEX idx_reports_reporter_status ON reports(reporter_id, status);

-- 코멘트 추가
COMMENT ON TABLE reports IS '사용자 및 자료 신고 정보';
COMMENT ON COLUMN reports.reporter_id IS '신고한 사용자 ID';
COMMENT ON COLUMN reports.reported_user_id IS '신고당한 사용자 ID';
COMMENT ON COLUMN reports.material_id IS '신고된 자료 ID (자료 신고시만)';
COMMENT ON COLUMN reports.report_type IS '신고 유형';
COMMENT ON COLUMN reports.description IS '신고 상세 내용';
COMMENT ON COLUMN reports.status IS '처리 상태';
COMMENT ON COLUMN reports.admin_note IS '관리자 처리 메모';
COMMENT ON COLUMN reports.resolved_at IS '처리 완료 시각';