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
    location geography(Point, 4326), --WGS 84 empleado para sistemas GPS
    category_id BIGINT NOT NULL,
    vendor_id BIGINT NOT NULL,
    deleted_at TIMESTAMP,
    suspended BOOLEAN DEFAULT FALSE,
    working_hours VARCHAR(255),

    CONSTRAINT fk_publications_category
    FOREIGN KEY (category_id)
    REFERENCES categories(id),

    CONSTRAINT fk_publications_vendor
    FOREIGN KEY (vendor_id)
    REFERENCES users(id)
    ON DELETE CASCADE
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

-- ======================
-- Inserción de datos de prueba
-- ======================

-- ======================
-- Inserción de usuarios de prueba
-- ======================

INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender)
VALUES
    ('0101010101', 'admin_user', 'password123', 'admin@example.com', '0999000001', 'Admin', 'User', 'M'),
    ('0202020202', 'moderator_user', 'password123', 'moderator@example.com', '0999000002', 'Moderator', 'User', 'F'),
    ('0303030303', 'seller_one', 'password123', 'seller1@example.com', '0999000003', 'Seller', 'One', 'M'),
    ('0404040404', 'seller_two', 'password123', 'seller2@example.com', '0999000004', 'Seller', 'Two', 'F'),
    ('0505050505', 'buyer_user', 'password123', 'buyer@example.com', '0999000005', 'Buyer', 'User', 'M');

-- ======================
-- Asignación de roles a usuarios
-- ======================


INSERT INTO users_roles (user_id, role_id) VALUES
                                               (1, 1), -- admin_user -> ROLE_ADMIN
                                               (2, 2), -- moderator_user -> ROLE_MODERATOR
                                               (3, 3), -- seller_one -> ROLE_SELLER
                                               (4, 3), -- seller_two -> ROLE_SELLER
                                               (5, 4); -- buyer_user -> ROLE_BUYER

-- ======================
-- Inserción de categorías iniciales
-- ======================
INSERT INTO categories (name) VALUES
                                  ('Electrónica'),
                                  ('Hogar'),
                                  ('Ropa'),
                                  ('Deportes'),
                                  ('Belleza y Salud'),
                                  ('Automotriz'),
                                  ('Alimentos y Bebidas'),
                                  ('Servicios Profesionales'),
                                  ('Transporte'),
                                  ('Educación');

-- ======================
-- Inserción de publicaciones
-- ======================
-- NOTA: Usando coordenadas ficticias
INSERT INTO publications (code, type, name, description, price, availability, status, location, category_id, vendor_id, working_hours)
VALUES
    ('PRD001', 'PRODUCT', 'Smartphone X', 'Último modelo de smartphone con 128GB', 699.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.4847, -0.1807), 4326), 1, 3, NULL),
    ('PRD002', 'PRODUCT', 'Sofá 3 Plazas', 'Sofá moderno de tela gris', 499.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.4850, -0.1810), 4326), 2, 3, NULL),
    ('SRV001', 'SERVICE', 'Clases de Yoga', 'Instructor certificado ofrece clases de yoga a domicilio', 20.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.4820, -0.1820), 4326), 4, 4, '08:00-12:00'),
    ('PRD003', 'PRODUCT', 'Bicicleta Montaña', 'Bicicleta MTB 29 pulgadas', 350.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.4860, -0.1830), 4326), 4, 3, NULL),
    ('SRV002', 'SERVICE', 'Reparación de PC', 'Servicio técnico de computadoras y laptops', 30.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.4870, -0.1840), 4326), 8, 4, '09:00-17:00'); -- Solo aquí tiene sentido si code es único

-- ======================
-- Inserción de imágenes de ejemplo para publicaciones
-- ======================
INSERT INTO publication_images (publication_id, path) VALUES
                                                          (1, 'images/smartphone1.jpg'),
                                                          (1, 'images/smartphone2.jpg'),
                                                          (2, 'images/sofa1.jpg'),
                                                          (3, 'images/yoga1.jpg'),
                                                          (4, 'images/bicicleta1.jpg'),
                                                          (5, 'images/pc_repair1.jpg');
