-- 기존 과목명을 새로운 과목명으로 변경
UPDATE study_material
SET subject = CASE
                  WHEN subject = '한국여성의역사' THEN '영상정보처리'
                  WHEN subject = '알고리즘' THEN '리눅스'
                  WHEN subject = '디지털논리회로' THEN '한국문화의이해'
                  WHEN subject = '보고듣고만지는현대사상' THEN '고전의현장과스토리'
                  ELSE subject
    END
WHERE subject IN ('한국여성의역사', '알고리즘', '디지털논리회로', '보고듣고만지는현대사상');

-- 새로운 과목 '디지털철학'은 별도 데이터가 필요하면 추가
-- INSERT INTO study_material (title, subject, exam_type, ...) VALUES (...);