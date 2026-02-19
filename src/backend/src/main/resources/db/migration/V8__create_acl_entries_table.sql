CREATE TABLE acl_entries (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    resource_type VARCHAR(20) NOT NULL CHECK (resource_type IN ('DOCUMENT', 'FOLDER')),
    resource_id   UUID        NOT NULL,
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    permission    VARCHAR(20) NOT NULL CHECK (permission IN ('READ', 'WRITE', 'ADMIN')),
    granted_by    VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (resource_type, resource_id, user_id, permission)
);

CREATE INDEX idx_acl_entries_resource_id ON acl_entries (resource_id);
CREATE INDEX idx_acl_entries_user_id ON acl_entries (user_id);
CREATE INDEX idx_acl_entries_resource_type_resource_id ON acl_entries (resource_type, resource_id);
