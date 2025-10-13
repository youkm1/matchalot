ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PENDING';

-- 기존 사용자들을 PENDING으로 설정 (관리자는 수동으로 ADMIN 설정 필요)
UPDATE users SET role = 'PENDING' WHERE role IS NULL;

-- 관리자 계정을 ADMIN으로 설정
UPDATE users SET role = 'ADMIN' WHERE email = 'youkm0806@sookmyung.ac.kr';

-- role 컬럼에 인덱스 추가 (조회 성능 향상)
CREATE INDEX idx_users_role ON users(role);

-- 가능한 role 값들에 대한 체크 제약 조건 추가
ALTER TABLE users ADD CONSTRAINT chk_user_role
    CHECK (role IN ('PENDING', 'MEMBER', 'ADMIN'));