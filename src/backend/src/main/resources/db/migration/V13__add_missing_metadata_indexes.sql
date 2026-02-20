-- Add missing indexes on document_metadata FK columns for join/filter performance
CREATE INDEX IF NOT EXISTS idx_document_metadata_business_unit_id ON document_metadata (business_unit_id);
CREATE INDEX IF NOT EXISTS idx_document_metadata_sbu_id ON document_metadata (sbu_id);
