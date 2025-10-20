-- Add temp_pdf_data column to study_material table for PDF Base64 approval system
ALTER TABLE study_material
ADD COLUMN temp_pdf_data TEXT;

-- Add comment for documentation
COMMENT ON COLUMN study_material.temp_pdf_data IS 'Temporary Base64 encoded PDF data for approval process. Cleared after approval/rejection.';