-- 과목명 변경 마이그레이션
-- 영상정보처리 -> 컴퓨터네트워크Ⅰ
-- 리눅스 -> 소셜미디어의이해와활용

UPDATE study_material 
SET subject = '컴퓨터네트워크Ⅰ'
WHERE subject = '영상정보처리';

UPDATE study_material 
SET subject = '소셜미디어의이해와활용'
WHERE subject = '리눅스';

-- 인덱스가 있다면 재생성 (선택사항)
-- 기존 제약조건이 있다면 업데이트 필요