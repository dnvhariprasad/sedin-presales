ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);

-- Seed default admin password (BCrypt hash of "Admin@123")
UPDATE users SET password_hash = '$2a$10$EqKcp1WFKAr1IEZAH6XQFOJLhFCiMOmo5YAxWjFhXkSCFGBqahVam' WHERE email = 'dev.admin@sedin.com';
