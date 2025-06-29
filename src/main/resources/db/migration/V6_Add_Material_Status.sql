ALTER TABLE study_material ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 기존 자료들을 APPROVED로 설정 (이미 존재하는 자료들은 승인된 것으로 간주)
UPDATE study_material SET status = 'APPROVED' WHERE status = 'PENDING';

-- status 컬럼에 인덱스 추가 (관리자 조회 성능 향상)
CREATE INDEX idx_study_materials_status ON study_material(status);

-- 가능한 status 값들에 대한 체크 제약 조건 추가
ALTER TABLE study_material ADD CONSTRAINT chk_material_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));