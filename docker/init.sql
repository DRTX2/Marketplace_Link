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

    location           GEOGRAPHY(Point, 4326),

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
    availability VARCHAR(20) NOT NULL  DEFAULT 'AVAILABLE', -- AVAILABLE, UNAVAILABLE
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
-- Tabla: favorite_publications
-- ======================
CREATE TABLE IF NOT EXISTS favorite_publications (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     user_id BIGINT NOT NULL,
                                                     publication_id BIGINT NOT NULL,
                                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                     CONSTRAINT uk_user_publication UNIQUE (user_id, publication_id),

    CONSTRAINT fk_favorite_publications_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_favorite_publications_publication
    FOREIGN KEY (publication_id)
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
                             ('ROLE_BUYER'),
                             ('ROLE_SYSTEM')
ON CONFLICT (name) DO NOTHING;

-- Asegurar extensiones requeridas
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =====================================
-- USUARIOS CON UBICACIÓN EN AMBATO
-- =====================================

-- Usuario del sistema → Parque Juan Montalvo
INSERT INTO users (
    cedula, username, password, email, phone, first_name, last_name, gender,
    account_status, email_verified_at, location
) VALUES (
             '9999999999',
             'system_user',
             crypt('system123', gen_salt('bf', 12)),
             'system@marketplace.local',
             '+0000000000',
             'System',
             'Bot',
             'OTHER',
             'ACTIVE',
             NOW(),
             ST_SetSRID(ST_MakePoint(-78.62935, -1.24222), 4326)
         ) ON CONFLICT (username) DO NOTHING;

-- Rol del sistema
INSERT INTO users_roles (user_id, role_id)
VALUES (
           (SELECT id FROM users WHERE username = 'system_user'),
           (SELECT id FROM roles WHERE name = 'ROLE_SYSTEM')
       ) ON CONFLICT (user_id, role_id) DO NOTHING;

-- Admin único → Catedral de Ambato
INSERT INTO users (
    cedula, username, password, email, phone, first_name, last_name, gender,
    account_status, email_verified_at, location
) VALUES (
             '0000000000',
             'admin',
             crypt('admin123', gen_salt('bf',12)),
             'admin@example.com',
             '+593000000000',
             'Admin',
             'Root',
             'MALE',
             'ACTIVE',
             NOW(),
             ST_SetSRID(ST_MakePoint(-78.628837, -1.241657), 4326)
         ) ON CONFLICT (username) DO NOTHING;

-- Otros usuarios con ubicaciones distintas en Ambato
INSERT INTO users (
    cedula, username, password, email, phone, first_name, last_name, gender,
    account_status, email_verified_at, location
) VALUES
      -- Parque Cevallos
      ('0202020202', 'JosueG', crypt('password123', gen_salt('bf',12)),
       'moderator@example.com', '0999000002', 'Josue', 'Garcia', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.62712, -1.24118), 4326)),

      -- Mall de los Andes
      ('0303030303', 'JoelB', crypt('password123', gen_salt('bf',12)),
       'seller1@example.com', '0999000003', 'Joel', 'Bonilla', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.62823, -1.26510), 4326)),

      -- UTA (Campus Huachi)
      ('0404040404', 'DavidM', crypt('password123', gen_salt('bf',12)),
       'seller2@example.com', '0999000004', 'David', 'Manjarres', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.62411, -1.26901), 4326)),

      -- Terminal Terrestre Ambato
      ('0505050505', 'DavidB', crypt('password123', gen_salt('bf',12)),
       'buyer@example.com', '0999000005', 'David', 'Barragan', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.61652, -1.23603), 4326))
ON CONFLICT (username) DO NOTHING;

-- =====================================
-- Asignación de roles fijos
-- =====================================
INSERT INTO users_roles (user_id, role_id) VALUES
    ((SELECT id FROM users WHERE username = 'admin'),   (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')),
    ((SELECT id FROM users WHERE username = 'JosueG'),  (SELECT id FROM roles WHERE name = 'ROLE_MODERATOR')),
    ((SELECT id FROM users WHERE username = 'JoelB'),   (SELECT id FROM roles WHERE name = 'ROLE_SELLER')),
    ((SELECT id FROM users WHERE username = 'DavidM'),  (SELECT id FROM roles WHERE name = 'ROLE_SELLER')),
    ((SELECT id FROM users WHERE username = 'DavidB'),  (SELECT id FROM roles WHERE name = 'ROLE_BUYER'))
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ======================
-- Inserción de 2 moderadores adicionales
-- ======================
INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender, account_status, email_verified_at)
VALUES
    ('0606060606', 'moderator_two', crypt('password123', gen_salt('bf',12)), 'moderator2@example.com', '0999000006', 'Moderator', 'Two', 'MALE', 'ACTIVE', NOW()),
    ('0707070707', 'moderator_three', crypt('password123', gen_salt('bf',12)), 'moderator3@example.com', '0999000007', 'Moderator', 'Three', 'FEMALE', 'ACTIVE', NOW())
    ON CONFLICT (username) DO NOTHING;

-- ======================
-- Asignación del rol ROLE_MODERATOR a los nuevos moderadores
-- ======================
INSERT INTO users_roles (user_id, role_id)
VALUES
    ((SELECT id FROM users WHERE username = 'moderator_two'), (SELECT id FROM roles WHERE name = 'ROLE_MODERATOR')),
    ((SELECT id FROM users WHERE username = 'moderator_three'), (SELECT id FROM roles WHERE name = 'ROLE_MODERATOR'))
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
    ('PRD001', 'PRODUCT', 'Smartphone 10', 'Último modelo de smartphone con 128GB', 699.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(102.00303584077051, 33.10074754335619), 4326), 1, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('PRD002', 'PRODUCT', 'Sofá 3 Plazas', 'Sofá moderno de tela gris', 499.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 2, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('SRV001', 'SERVICE', 'Clases de Yoga', 'Instructor certificado ofrece clases de yoga a domicilio', 20.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 4, (SELECT id FROM users WHERE username = 'DavidM'), 'Lunes a Viernes de: 08:00-12:00'),
    ('PRD003', 'PRODUCT', 'Bicicleta Montaña', 'Bicicleta MTB 29 pulgadas', 350.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 4, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('SRV002', 'SERVICE', 'Reparación de PC', 'Servicio técnico de computadoras y laptops', 30.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.62415372809278, -1.2680301243556702), 4326), 8, (SELECT id FROM users WHERE username = 'DavidM'), 'Lunes a Viernes de: 09:00-17:00');

-- ======================
-- Inserción de imágenes de ejemplo para publicaciones
-- ======================
INSERT INTO publication_images (publication_id, path) VALUES
                                                          (1, 'phone.webp'),
                                                          (2, 'sofa.jpg'),
                                                          (3, 'yoga.jpg'),
                                                          (4, 'bicicleta.jpg'),
                                                          (4, 'bicicleta2.jpg'),
                                                          (5, 'reparacion.jpg');
-- ======================
-- Inserción de favoritos de prueba
-- ======================
INSERT INTO favorite_publications (user_id, publication_id, created_at)
VALUES
    ((SELECT id FROM users WHERE username = 'DavidB'), 1, NOW()),
    ((SELECT id FROM users WHERE username = 'DavidB'), 2, NOW()),
    ((SELECT id FROM users WHERE username = 'JoelB'), 3, NOW()),
    ((SELECT id FROM users WHERE username = 'JosueG'), 1, NOW())
    ON CONFLICT (user_id, publication_id) DO NOTHING;


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
                            moderator_comment TEXT,
                            decision VARCHAR(20) CHECK (decision IN ('APPROVED','REJECTED')),


                            FOREIGN KEY (publication_id) REFERENCES publications(id),
                            FOREIGN KEY (moderator_id) REFERENCES users(id)
);

-- Mas de 3 reportes, el producto se oculta.
CREATE TABLE reports (
                         id BIGSERIAL PRIMARY KEY ,
                         incidence_id BIGINT NOT NULL,
                         reporter_id BIGINT NOT NULL,                     -- comprador que reporta o puede ser el propio sistema.
                         reason VARCHAR(100) NOT NULL,              -- tipo de reporte
                         comment TEXT,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         source VARCHAR(20) CHECK (source IN ('USER', 'SYSTEM')) DEFAULT 'USER',
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
                         status VARCHAR(20) CHECK (
                             status IN (
                                        'PENDING',          -- apelación creada, esperando asignación de nuevo moderador
                                        'ASSIGNED',         -- nuevo moderador asignado, en revisión
                                        'FAILED_NO_MOD',    -- no hay moderadores disponibles
                                        'REVIEWED'          -- revisión completada (decision final tomada)
                                 )
                             ) DEFAULT 'PENDING',
                         new_moderator_id BIGINT,				 -- para elegir el nuevo moderador se hace cálculos en el back (puede ser nulo si no hay moderadores)
                         final_decision VARCHAR(20) CHECK (final_decision IN ('ACCEPTED','REJECTED')),
                         final_decision_at TIMESTAMP,

                         CONSTRAINT unique_incidence UNIQUE (incidence_id),
                         FOREIGN KEY (incidence_id) REFERENCES incidences(id),
                         FOREIGN KEY (seller_id) REFERENCES users(id),
                         FOREIGN KEY (new_moderator_id) REFERENCES users(id)
);

