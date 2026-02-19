CREATE TABLE document_versions (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID          NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number  INTEGER       NOT NULL,
    file_path       VARCHAR(1000) NOT NULL,
    file_name       VARCHAR(500)  NOT NULL,
    file_size       BIGINT,
    content_type    VARCHAR(255),
    uploaded_by     VARCHAR(255),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE (document_id, version_number)
);

CREATE INDEX idx_document_versions_document_id ON document_versions (document_id);
