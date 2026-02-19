CREATE TABLE documents (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    title                   VARCHAR(500) NOT NULL,
    folder_id               UUID         REFERENCES folders(id) ON DELETE SET NULL,
    document_type_id        UUID         REFERENCES document_types(id) ON DELETE SET NULL,
    customer_name           VARCHAR(255),
    document_date           DATE,
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    rag_indexed             BOOLEAN      DEFAULT FALSE,
    current_version_number  INTEGER      DEFAULT 0,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255)
);

CREATE INDEX idx_documents_folder_id ON documents (folder_id);
CREATE INDEX idx_documents_document_type_id ON documents (document_type_id);
CREATE INDEX idx_documents_customer_name ON documents (customer_name);
CREATE INDEX idx_documents_status ON documents (status);
