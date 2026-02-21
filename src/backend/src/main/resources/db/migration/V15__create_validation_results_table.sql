CREATE TABLE IF NOT EXISTS case_study_validation_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    agent_id UUID NOT NULL REFERENCES case_study_agents(id),
    is_valid BOOLEAN NOT NULL DEFAULT false,
    validation_details JSONB,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_cs_validation_doc_version ON case_study_validation_results (document_version_id);
CREATE INDEX IF NOT EXISTS idx_cs_validation_agent ON case_study_validation_results (agent_id);
