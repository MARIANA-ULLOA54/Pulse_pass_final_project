-- SOLO base pulsepass_e2e. Ejecutar con psql DESPUÉS de Flyway V1–V4.
-- Requiere pgcrypto para generar hashes BCrypt sin guardar contraseñas.
-- Uso: psql -h localhost -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
\set ON_ERROR_STOP on

BEGIN;

DO $$
BEGIN
    IF current_database() <> 'pulsepass_e2e' THEN
        RAISE EXCEPTION 'Conecta psql a la base pulsepass_e2e antes de ejecutar el seed.';
    END IF;

    IF to_regclass('venues') IS NULL OR to_regclass('users') IS NULL
        OR to_regclass('user_credentials') IS NULL OR to_regclass('user_roles') IS NULL THEN
        RAISE EXCEPTION 'Faltan tablas de PulsePass en pulsepass_e2e.'
            USING HINT = 'Arranca el backend con DB_URL apuntando a pulsepass_e2e para aplicar Flyway V1 a V4.';
    END IF;
END;
$$;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
\prompt 'Contraseña STAFF (min 12 caracteres): ' staff_pass
\prompt 'Contraseña ADMIN (min 12 caracteres): ' admin_pass
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-E2E', 'Auditorio E2E', 'Santa Marta', 'Local de pruebas', 500, TRUE)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users (username, email, active)
VALUES
    ('staff_e2e', 'staff_e2e@example.test', TRUE),
    ('admin_e2e', 'admin_e2e@example.test', TRUE)
ON CONFLICT (username) DO NOTHING;

INSERT INTO user_credentials (user_id, password_hash)
SELECT id, crypt(:'staff_pass', gen_salt('bf', 10))
FROM users
WHERE username = 'staff_e2e'
ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO user_credentials (user_id, password_hash)
SELECT id, crypt(:'admin_pass', gen_salt('bf', 10))
FROM users
WHERE username = 'admin_e2e'
ON CONFLICT (user_id) DO UPDATE SET password_hash = EXCLUDED.password_hash;

INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF'
FROM users
WHERE username = 'staff_e2e'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN'
FROM users
WHERE username = 'admin_e2e'
ON CONFLICT DO NOTHING;

-- Los artistas iniciales provienen de V2; consultar /api/artists/active y actualizar artistId.
COMMIT;
