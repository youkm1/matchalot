--1. 기존 제약조건 제거
ALTER TABLE study_material DROP CONSTRAINT IF EXISTS study_material_exam_type_check;
--2. 새로 한글 제약조건 추가
ALTER TABLE study_material ADD CONSTRAINT study_material_exam_type_check
    CHECK ( exam_type IN ('중간고사', '기말고사', 'MIDTERM', 'FINAL') );
--3. 기존 영어 -> 한글 변환
UPDATE study_material
SET exam_type = CASE
                    WHEN exam_type = 'MIDTERM' THEN '중간고사'
                    WHEN exam_type = 'FINAL' THEN '기말고사'
                    ELSE exam_type
    END
WHERE exam_type IN ('MIDTERM', 'FINAL');

--4. 기존 영어 subject 데이터를 한글로 변환 (있다면)
UPDATE study_material
SET subject = CASE
                  WHEN subject = 'KOREAN_WOMEN_HISTORY' THEN '한국여성의역사'
                  WHEN subject = 'ALGORITHM' THEN '알고리즘'
                  WHEN subject = 'DIGITAL_LOGIC_CIRCUIT' THEN '디지털논리회로'
                  WHEN subject = 'STATISTICS_INTRODUCTION' THEN '통계학입문'
                  ELSE subject
    END
WHERE subject IN ('KOREAN_WOMEN_HISTORY', 'ALGORITHM', 'DIGITAL_LOGIC_CIRCUIT', 'STATISTICS_INTRODUCTION');