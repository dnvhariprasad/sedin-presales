-- Seed a default user for initial login verification.
-- No password is stored because authentication uses Entra ID.
INSERT INTO users (email, display_name, role, status, created_by, updated_by)
VALUES ('dev.admin@sedin.com', 'Dev Admin', 'ADMIN', 'ACTIVE', 'system', 'system')
ON CONFLICT (email) DO NOTHING;

