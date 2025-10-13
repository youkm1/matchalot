-- 알림 테이블 생성
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    related_entity_id VARCHAR(100),
    
    CONSTRAINT fk_notification_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_type ON notifications(type);

-- 알림 타입에 대한 체크 제약
ALTER TABLE notifications ADD CONSTRAINT chk_notification_type 
CHECK (type IN ('USER_PROMOTED', 'MATERIAL_APPROVED', 'MATERIAL_REJECTED', 'MATCH_COMPLETED', 'MATCH_REQUEST_RECEIVED', 'SYSTEM'));

-- 코멘트 추가
COMMENT ON TABLE notifications IS '사용자 알림 테이블';
COMMENT ON COLUMN notifications.id IS '알림 ID';
COMMENT ON COLUMN notifications.user_id IS '알림 대상 사용자 ID';
COMMENT ON COLUMN notifications.type IS '알림 유형';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.message IS '알림 메시지';
COMMENT ON COLUMN notifications.is_read IS '읽음 여부';
COMMENT ON COLUMN notifications.created_at IS '생성 시간';
COMMENT ON COLUMN notifications.related_entity_id IS '관련 엔티티 ID (매칭 ID, 자료 ID 등)';