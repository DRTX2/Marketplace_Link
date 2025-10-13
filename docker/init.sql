-- =========================================================
--  Creación de tablas base para Marketplace_Link
--  Compatible con PostgreSQL 16 + PostGIS
-- =========================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;
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
CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    cedula           VARCHAR(10)  NOT NULL,
    username         VARCHAR(100) NOT NULL,
    password         VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    gender           VARCHAR(10),
    account_status   VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at TIMESTAMP NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT uk_user_username UNIQUE (username),
    CONSTRAINT uk_user_email    UNIQUE (email),
    CONSTRAINT uk_user_phone    UNIQUE (phone),
    CONSTRAINT uk_user_cedula   UNIQUE (cedula)
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
           ON DELETE CASCADE,

   CONSTRAINT uk_users_roles_user_id_role_id UNIQUE (user_id, role_id)
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
-- Tabla: email_verification_tokens
-- ======================
CREATE TABLE email_verification_tokens (
                                           id          BIGSERIAL PRIMARY KEY,
                                           user_id     BIGINT       NOT NULL,
                                           token       VARCHAR(100) NOT NULL UNIQUE,
                                           expires_at  TIMESTAMP    NOT NULL,
                                           consumed_at TIMESTAMP,
                                           CONSTRAINT fk_email_verif_user
                                               FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_email_verif_user   ON email_verification_tokens(user_id);
CREATE INDEX idx_email_verif_expiry ON email_verification_tokens(expires_at);

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
-- ROLES
-- ======================
INSERT INTO roles (name) VALUES
                             ('ROLE_ADMIN'),
                             ('ROLE_MODERATOR'),
                             ('ROLE_SELLER'),
                             ('ROLE_BUYER')
ON CONFLICT (name) DO NOTHING;

-- ======================
-- Inserción de usuarios de prueba
-- ======================
-- ================
-- Seed: single admin + sample users (bcrypt via crypt)
-- ================

-- Admin único
INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender, account_status, email_verified_at)
VALUES (
           '0000000000',
           'admin',
           crypt('admin123', gen_salt('bf',12)),
           'admin@example.com',
           '+593000000000',
           'Admin',
           'Root',
           'MALE',
           'ACTIVE',
           NOW()
       )
ON CONFLICT (username) DO NOTHING;

-- Otros usuarios
INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender, account_status, email_verified_at)
VALUES
    ('0202020202', 'moderator_user', crypt('password123', gen_salt('bf',12)), 'moderator@example.com', '0999000002', 'Moderator','User','FEMALE','ACTIVE', NOW()),
    ('0303030303', 'seller_one',     crypt('password123', gen_salt('bf',12)), 'seller1@example.com',   '0999000003', 'Seller',   'One', 'MALE',  'ACTIVE', NOW()),
    ('0404040404', 'seller_two',     crypt('password123', gen_salt('bf',12)), 'seller2@example.com',   '0999000004', 'Seller',   'Two', 'FEMALE','ACTIVE', NOW()),
    ('0505050505', 'buyer_user',     crypt('password123', gen_salt('bf',12)), 'buyer@example.com',     '0999000005', 'Buyer',    'User','MALE',  'ACTIVE', NOW())
ON CONFLICT (username) DO NOTHING;

-- ================
-- Role mapping by fixed ids (ensure PKs match this order)
-- ================
INSERT INTO users_roles (user_id, role_id) VALUES
                                               (1, 1), -- admin           -> ROLE_ADMIN
                                               (2, 2), -- moderator_user  -> ROLE_MODERATOR
                                               (3, 3), -- seller_one      -> ROLE_SELLER
                                               (4, 3), -- seller_two      -> ROLE_SELLER
                                               (5, 4)  -- buyer_user      -> ROLE_BUYER
ON CONFLICT (user_id, role_id) DO NOTHING;

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
    ('PRD001', 'PRODUCT', 'Smartphone X', 'Último modelo de smartphone con 128GB', 699.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 1, 3, NULL),
    ('PRD002', 'PRODUCT', 'Sofá 3 Plazas', 'Sofá moderno de tela gris', 499.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 2, 3, NULL),
    ('SRV001', 'SERVICE', 'Clases de Yoga', 'Instructor certificado ofrece clases de yoga a domicilio', 20.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 4, 4, '08:00-12:00'),
    ('PRD003', 'PRODUCT', 'Bicicleta Montaña', 'Bicicleta MTB 29 pulgadas', 350.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 4, 3, NULL),
    ('SRV002', 'SERVICE', 'Reparación de PC', 'Servicio técnico de computadoras y laptops', 30.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 8, 4, '09:00-17:00');

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

--- Para el flujo de moderación

-- Se genera una automaticmaente cuando se reporta el producto. Es decir, se genera un reporte e incidencia como primer momento.
CREATE TABLE incidences (
                            id  BIGSERIAL PRIMARY KEY ,
                            publication_id BIGINT NOT NULL,
                            status VARCHAR(20) CHECK (status IN ('OPEN', 'UNDER_REVIEW','APPEALED','RESOLVED')) DEFAULT 'OPEN',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_report_at TIMESTAMP,
                            auto_closed BOOLEAN DEFAULT FALSE,
                            moderator_id BIGINT,
                            decision VARCHAR(20) CHECK (decision IN ('APPROVED','REJECTED')),

                            FOREIGN KEY (publication_id) REFERENCES publications(id),
                            FOREIGN KEY (moderator_id) REFERENCES users(id)
);

-- Mas de 3 reportes, el producto se oculta.
CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY ,
                         incidence_id BIGINT NOT NULL,
                         reporter_id BIGINT NOT NULL,                     -- comprador que reporta
                         reason VARCHAR(100) NOT NULL,              -- tipo de reporte
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                         FOREIGN KEY (incidence_id) REFERENCES incidences(id),
                         FOREIGN KEY (reporter_id) REFERENCES users(id)
);

-- Solo se puede apelar una vez.
CREATE TABLE appeals (
                         id  BIGSERIAL PRIMARY KEY ,
                         incidence_id BIGINT NOT NULL,                          -- sigue apuntando a la incidencia, SOLO una por incidencia (UNIQUE)
                         seller_id BIGINT NOT NULL,				 -- el vendedor que hace la apelacion
                         reason TEXT NOT NULL,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         new_moderator_id BIGINT,				 -- para elegir el nuevo moderador se hace cálculos en el back
                         final_decision VARCHAR(20) CHECK (final_decision IN ('ACCEPTED','REJECTED')),
                         final_decision_at TIMESTAMP,

                         CONSTRAINT unique_incidence UNIQUE (incidence_id),
                         FOREIGN KEY (incidence_id) REFERENCES incidences(id),
                         FOREIGN KEY (seller_id) REFERENCES users(id),
                         FOREIGN KEY (new_moderator_id) REFERENCES users(id)
);