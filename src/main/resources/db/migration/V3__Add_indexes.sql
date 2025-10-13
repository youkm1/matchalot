CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_study_material_subject ON study_material(subject);
CREATE INDEX idx_study_material_uploader ON study_material(uploader_id);
CREATE INDEX idx_study_material_subject_exam ON study_material(subject, exam_type);