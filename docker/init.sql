-- =========================================================
--  Creación de tablas base para Marketplace_Link
--  Compatible con PostgreSQL 16 + PostGIS
-- =========================================================

-- ======================
-- Extensión PostGIS
-- ======================
CREATE EXTENSION IF NOT EXISTS postgis;

-- ======================
-- Tabla: roles
-- ======================
CREATE TABLE IF NOT EXISTS roles (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) UNIQUE NOT NULL
    );

-- ======================
-- Tabla: users
-- ======================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     cedula VARCHAR(10) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10),
    account_status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email UNIQUE (email),
    CONSTRAINT uk_user_phone UNIQUE (phone),
    CONSTRAINT uk_user_cedula UNIQUE (cedula)
    );

-- ======================
-- Tabla intermedia: users_roles
-- ======================
CREATE TABLE IF NOT EXISTS users_roles (
                                           user_id BIGINT NOT NULL,
                                           role_id BIGINT NOT NULL,
                                           PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_users_roles_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE,

    CONSTRAINT fk_users_roles_role
    FOREIGN KEY (role_id)
    REFERENCES roles (id)
    ON DELETE CASCADE
    );

-- ======================
-- Tabla: password_reset_token
-- ======================
CREATE TABLE IF NOT EXISTS password_reset_token (
                                                    id BIGSERIAL PRIMARY KEY,
                                                    user_id BIGINT NOT NULL,
                                                    token VARCHAR(255) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    expiration TIMESTAMP NOT NULL,

    CONSTRAINT fk_password_reset_token_user
    FOREIGN KEY (user_id)
    REFERENCES users (id)
    ON DELETE CASCADE
    );

-- ======================
-- Datos iniciales
-- ======================
INSERT INTO roles (name) VALUES
                             ('ROLE_ADMIN'),
                             ('ROLE_MODERATOR'),
                             ('ROLE_SELLER'),
                             ('ROLE_BUYER')
    ON CONFLICT (name) DO NOTHING;

-- ======================
-- Trigger para updated_at
-- ======================
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- =========================================================
-- Tablas para Marketplace
-- =========================================================

-- ======================
-- Tabla: categories
-- ======================
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGSERIAL PRIMARY KEY,
                                          name VARCHAR(255) NOT NULL
    );

-- ======================
-- Tabla: vendors
-- ======================
CREATE TABLE IF NOT EXISTS vendors (
                                       id BIGSERIAL PRIMARY KEY
);

-- ======================
-- Tabla: publications
-- ======================
CREATE TABLE IF NOT EXISTS publications (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE,
    type VARCHAR(20) NOT NULL, -- PRODUCT or SERVICE
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    availability VARCHAR(20) NOT NULL  DEFAULT 'AVAILABLE', -- AVAILABLE, NOT_AVAILABLE
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE', -- VISIBLE,  UNDER_REVIEW , BLOCKED,
    publication_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location geography(Point, 4326), --WGS 84 Empleado para sistemas GPS
    category_id BIGINT,
    vendor_id BIGINT, -- referencia al vendedor, de momento sin llave foranea
    deleted_at TIMESTAMP,
    suspended BOOLEAN DEFAULT FALSE,
    working_hours VARCHAR(255),

    CONSTRAINT fk_publications_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id),
    -- CONSTRAINT fk_publications_vendor FOREIGN KEY (vendor_id)
    --     REFERENCES vendors(id)
    );

-- ======================
-- Tabla: publication_images
-- ======================
CREATE TABLE IF NOT EXISTS publication_images (
   id BIGSERIAL PRIMARY KEY,
   publication_id BIGINT NOT NULL,
   path VARCHAR(255) NOT NULL,
    CONSTRAINT fk_publication_images_publication FOREIGN KEY (publication_id)
    REFERENCES publications(id)
    ON DELETE CASCADE
    );
