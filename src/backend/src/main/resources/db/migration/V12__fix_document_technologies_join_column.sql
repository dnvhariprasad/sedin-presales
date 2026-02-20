-- Fix document_technologies join column: change document_id -> document_metadata_id
-- The JPA entity DocumentMetadata maps ManyToMany with document_metadata_id,
-- but V5 created the table with document_id referencing documents(id).

DROP INDEX IF EXISTS idx_document_technologies_technology_id;
DROP TABLE IF EXISTS document_technologies;

CREATE TABLE document_technologies (
    document_metadata_id UUID NOT NULL REFERENCES document_metadata(id) ON DELETE CASCADE,
    technology_id        UUID NOT NULL REFERENCES technologies(id) ON DELETE CASCADE,
    PRIMARY KEY (document_metadata_id, technology_id)
);

CREATE INDEX idx_document_technologies_technology_id ON document_technologies (technology_id);
