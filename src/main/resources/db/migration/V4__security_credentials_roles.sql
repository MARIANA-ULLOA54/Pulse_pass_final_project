CREATE TABLE user_credentials (user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE, password_hash VARCHAR(100) NOT NULL, password_changed_at TIMESTAMP);
CREATE TABLE user_roles (user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, role VARCHAR(10) NOT NULL CHECK (role IN ('USER','STAFF','ADMIN')), PRIMARY KEY(user_id,role));
-- Usuarios anteriores no reciben claves por defecto; su activacion requiere proceso seguro.
