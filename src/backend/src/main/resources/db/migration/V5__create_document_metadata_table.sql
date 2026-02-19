CREATE TABLE document_metadata (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id      UUID UNIQUE NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    domain_id        UUID REFERENCES domains(id) ON DELETE SET NULL,
    industry_id      UUID REFERENCES industries(id) ON DELETE SET NULL,
    business_unit_id UUID REFERENCES business_units(id) ON DELETE SET NULL,
    sbu_id           UUID REFERENCES sbus(id) ON DELETE SET NULL
);

CREATE INDEX idx_document_metadata_document_id ON document_metadata (document_id);
CREATE INDEX idx_document_metadata_domain_id ON document_metadata (domain_id);
CREATE INDEX idx_document_metadata_industry_id ON document_metadata (industry_id);

CREATE TABLE document_technologies (
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    technology_id UUID NOT NULL REFERENCES technologies(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, technology_id)
);

CREATE INDEX idx_document_technologies_technology_id ON document_technologies (technology_id);
