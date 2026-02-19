CREATE TABLE renditions (
    id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id  UUID          NOT NULL REFERENCES document_versions(id) ON DELETE CASCADE,
    rendition_type       VARCHAR(30)   NOT NULL CHECK (rendition_type IN ('PDF', 'SUMMARY', 'FORMATTED')),
    file_path            VARCHAR(1000) NOT NULL,
    file_size            BIGINT,
    status               VARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    error_message        TEXT,
    created_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_renditions_document_version_id ON renditions (document_version_id);
CREATE INDEX idx_renditions_rendition_type ON renditions (rendition_type);
CREATE INDEX idx_renditions_status ON renditions (status);
