-- ============================================================
-- V028: Reset app_user — seed admin + agency users
-- Passwords are BCrypt hashed (Spring Security default)
-- ============================================================
SET search_path TO intermodal, public;

-- Clear existing users
DELETE FROM app_user;

-- admin@thy.com / admin (BCrypt hash)
INSERT INTO app_user (id, email, password_hash, role, display_name, is_active)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@thy.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    'System Admin',
    TRUE
);

-- agency@thy.com / agency (BCrypt hash)
INSERT INTO app_user (id, email, password_hash, role, display_name, is_active)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'agency@thy.com',
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Kz2pHjAA7R3REcCyMorWS',
    'AGENCY',
    'Agency User',
    TRUE
);
