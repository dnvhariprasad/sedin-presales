-- Fix document_technologies join column: change document_id -> document_metadata_id
-- The JPA entity DocumentMetadata maps ManyToMany with document_metadata_id,
-- but V5 created the table with document_id referencing documents(id).
-- Preserve existing data by mapping through document_metadata.

-- Step 1: Rename old table to preserve data
ALTER TABLE document_technologies RENAME TO document_technologies_old;

-- Step 2: Create corrected table
CREATE TABLE document_technologies (
    document_metadata_id UUID NOT NULL REFERENCES document_metadata(id) ON DELETE CASCADE,
    technology_id        UUID NOT NULL REFERENCES technologies(id) ON DELETE CASCADE,
    PRIMARY KEY (document_metadata_id, technology_id)
);

CREATE INDEX idx_document_technologies_technology_id ON document_technologies (technology_id);

-- Step 3: Migrate existing data by joining through document_metadata
INSERT INTO document_technologies (document_metadata_id, technology_id)
SELECT dm.id, dto.technology_id
FROM document_technologies_old dto
JOIN document_metadata dm ON dm.document_id = dto.document_id
ON CONFLICT DO NOTHING;

-- Step 4: Drop old table
DROP TABLE document_technologies_old;
