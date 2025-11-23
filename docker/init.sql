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

CREATE INDEX idx_users_account_status
    ON users (account_status);

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
-- Tabla:
-- tions
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
    previous_status VARCHAR(20),
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
    deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
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
       'josuegarcab2@hotmail.com', '0999000002', 'Josue', 'Garcia', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.62712, -1.24118), 4326)),

      -- Mall de los Andes
      ('0303030303', 'JoelB', crypt('password123', gen_salt('bf',12)),
       'pruebasjos05@gmail.com', '0999000003', 'Joel', 'Bonilla', 'MALE',
       'ACTIVE', NOW(), ST_SetSRID(ST_MakePoint(-78.62823, -1.26510), 4326)),

      -- UTA (Campus Huachi)
      ('0404040404', 'DavidM', crypt('password123', gen_salt('bf',12)),
       'pruebasjos07@gmail.com', '0999000004', 'David', 'Manjarres', 'MALE',
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
    ((SELECT id FROM users WHERE username = 'admin'),   (SELECT id FROM roles WHERE name = 'ROLE_ADMIN')), -- admin@example.com
    ((SELECT id FROM users WHERE username = 'JosueG'),  (SELECT id FROM roles WHERE name = 'ROLE_MODERATOR')), -- josuegarcab2@hotmail.com
    ((SELECT id FROM users WHERE username = 'JoelB'),   (SELECT id FROM roles WHERE name = 'ROLE_SELLER')), -- pruebasjos05@gmail.com
    ((SELECT id FROM users WHERE username = 'DavidM'),  (SELECT id FROM roles WHERE name = 'ROLE_SELLER')), -- pruebasjos07@gmail.com
    ((SELECT id FROM users WHERE username = 'DavidB'),  (SELECT id FROM roles WHERE name = 'ROLE_BUYER')) -- buyer@example.com
ON CONFLICT (user_id, role_id) DO NOTHING;

-- ======================
-- Inserción de 2 moderadores adicionales
-- ======================
INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender, account_status, email_verified_at)
VALUES
    ('0606060606', 'moderator_two', crypt('password123', gen_salt('bf',12)), 'josuegarcab2@gmail.com', '0999000006', 'Moderator', 'Two', 'MALE', 'ACTIVE', NOW()),
    ('0707070707', 'moderator_three', crypt('password123', gen_salt('bf',12)), 'pruebasjos04@gmail.com', '0999000007', 'Moderator', 'Three', 'FEMALE', 'ACTIVE', NOW())
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
    ('PRD004', 'PRODUCT', 'Laptop Pro 15"', 'Laptop con procesador i7 y 16GB RAM', 1099.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.615124, -1.270321), 4326), 1, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('PRD005', 'PRODUCT', 'Televisor 55"', 'Smart TV 4K UHD con HDR10+', 799.00, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.610052, -1.265012), 4326), 1, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('PRD006', 'PRODUCT', 'Auriculares Bluetooth', 'Auriculares inalámbricos con cancelación de ruido', 129.99, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.617845, -1.264500), 4326), 1, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('PRD007', 'PRODUCT', 'Mouse Gamer RGB', 'Mouse ergonómico con luces RGB y 6 botones', 39.90, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.624500, -1.268700), 4326), 2, (SELECT id FROM users WHERE username = 'JoelB'), NULL),
    ('PRD008', 'PRODUCT', 'Teclado Mecánico', 'Teclado mecánico retroiluminado con switches azules', 79.90, 'AVAILABLE', 'VISIBLE', ST_SetSRID(ST_MakePoint(-78.624200, -1.268100), 4326), 2, (SELECT id FROM users WHERE username = 'JoelB'), NULL);

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
                            public_ui UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
                            publication_id BIGINT NOT NULL,
                            status VARCHAR(20) CHECK (status IN ('OPEN', 'PENDING_REVIEW','UNDER_REVIEW','APPEALED','RESOLVED')) DEFAULT 'OPEN',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            auto_closed BOOLEAN DEFAULT FALSE,
                            moderator_id BIGINT,
                            moderator_comment TEXT,
                            decision VARCHAR(20) CHECK (decision IN ('APPROVED','REJECTED', 'PENDING')) DEFAULT 'PENDING',

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
                         final_decision VARCHAR(20) CHECK (final_decision IN ('ACCEPTED','REJECTED', 'PENDING')) DEFAULT 'PENDING',
                         final_decision_at TIMESTAMP,

                         CONSTRAINT unique_incidence UNIQUE (incidence_id),
                         FOREIGN KEY (incidence_id) REFERENCES incidences(id),
                         FOREIGN KEY (seller_id) REFERENCES users(id),
                         FOREIGN KEY (new_moderator_id) REFERENCES users(id)
);

-- =========================================================
-- Datos de prueba para Incidencias, Reportes y Apelaciones
-- =========================================================

-- ======================
-- Incidencia 1: Publicación bloqueada por sistema (3 reportes automáticos)
-- ======================
INSERT INTO incidences (publication_id, status, moderator_id, decision, auto_closed, moderator_comment)
VALUES
    (1, 'RESOLVED', (SELECT id FROM users WHERE username = 'JosueG'), 'REJECTED', TRUE, 'Producto con contenido inapropiado - bloqueado automáticamente por 3 reportes')
ON CONFLICT DO NOTHING;

-- Asegurar que el usuario system_user exista antes de insertar reportes del sistema
INSERT INTO users (cedula, username, password, email, phone, first_name, last_name, gender, account_status, email_verified_at, location)
SELECT
    '9999999999',
    'system_user',
    crypt('system123', gen_salt('bf',12)),
    'system@marketplace.local',
    '+0000000000',
    'System',
    'Bot',
    'OTHER',
    'ACTIVE',
    NOW(),
    ST_SetSRID(ST_MakePoint(-78.62935, -1.24222), 4326)
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'system_user');

-- Reportes del sistema para incidencia 1
INSERT INTO reports (incidence_id, reporter_id, reason, comment, source)
VALUES
    (1, (SELECT id FROM users WHERE username = 'system_user'), 'Contenido inapropiado', 'Detección automática de palabras prohibidas', 'SYSTEM'),
    (1, (SELECT id FROM users WHERE username = 'system_user'), 'Información engañosa', 'Precio sospechoso detectado por algoritmo', 'SYSTEM'),
    (1, (SELECT id FROM users WHERE username = 'system_user'), 'Imagen inapropiada', 'Imagen no cumple con políticas', 'SYSTEM')
ON CONFLICT DO NOTHING;

-- ======================
-- Incidencia 2: Reportada por usuarios, bajo revisión
-- ======================
INSERT INTO incidences (publication_id, status, moderator_id, decision)
VALUES
    (2, 'UNDER_REVIEW', (SELECT id FROM users WHERE username = 'JosueG'), 'PENDING')
ON CONFLICT DO NOTHING;

-- Reportes de usuarios para incidencia 2
INSERT INTO reports (incidence_id, reporter_id, reason, comment, source)
VALUES
    (2, (SELECT id FROM users WHERE username = 'DavidB'), 'Producto defectuoso', 'El producto no funciona como se describe', 'USER'),
    (2, (SELECT id FROM users WHERE username = 'DavidM'), 'Precio incorrecto', 'El precio en la tienda física es diferente', 'USER')
ON CONFLICT DO NOTHING;

-- ======================
-- Incidencia 3: Abierta, pendiente de asignación de moderador
-- ======================
INSERT INTO incidences (publication_id, status)
VALUES
    (3, 'OPEN')
ON CONFLICT DO NOTHING;

-- Reporte inicial para incidencia 3
INSERT INTO reports (incidence_id, reporter_id, reason, comment, source)
VALUES
    (3, (SELECT id FROM users WHERE username = 'DavidB'), 'Vendedor no responde', 'Intenté contactar al vendedor sin respuesta', 'USER')
ON CONFLICT DO NOTHING;

-- ======================
-- Incidencia 4: Con apelación pendiente
-- ======================
INSERT INTO incidences (publication_id, status, moderator_id, decision)
VALUES
    (4, 'APPEALED', (SELECT id FROM users WHERE username = 'moderator_two'), 'REJECTED')
ON CONFLICT DO NOTHING;

-- Reportes para incidencia 4
INSERT INTO reports (incidence_id, reporter_id, reason, comment, source)
VALUES
    (4, (SELECT id FROM users WHERE username = 'DavidB'), 'Producto falsificado', 'La bicicleta parece ser una copia', 'USER'),
    (4, (SELECT id FROM users WHERE username = 'DavidM'), 'Descripción engañosa', 'Las especificaciones no coinciden', 'USER')
ON CONFLICT DO NOTHING;

-- Apelación del vendedor para incidencia 4
INSERT INTO appeals (incidence_id, seller_id, reason, status, new_moderator_id)
VALUES
    (4, 
     (SELECT id FROM users WHERE username = 'JoelB'), 
     'Mi producto es auténtico. Tengo facturas y certificados de garantía que demuestran su originalidad. Los compradores malinterpretaron la descripción.',
     'ASSIGNED',
     (SELECT id FROM users WHERE username = 'moderator_three'))
ON CONFLICT DO NOTHING;

-- ======================
-- Incidencia 5: Resuelta y aprobada
-- ======================
INSERT INTO incidences (publication_id, status, moderator_id, decision, moderator_comment)
VALUES
    (5, 'RESOLVED', (SELECT id FROM users WHERE username = 'moderator_three'), 'APPROVED', 'Reporte sin fundamento. Producto cumple con políticas.')
ON CONFLICT DO NOTHING;

-- Reporte sin fundamento para incidencia 5
INSERT INTO reports (incidence_id, reporter_id, reason, comment, source)
VALUES
    (5, (SELECT id FROM users WHERE username = 'DavidB'), 'No me gustó', 'El teclado no es de mi agrado', 'USER')
ON CONFLICT DO NOTHING;

-- ======================
-- Incidencia 6: Con apelación rechazada (final)
-- ======================
INSERT INTO incidences (publication_id, status, moderator_id, decision, moderator_comment)
VALUES
    (1, 'RESOLVED', (SELECT id FROM users WHERE username = 'JosueG'), 'REJECTED', 'Después de revisar la apelación, se mantiene la decisión de bloqueo.')
ON CONFLICT DO NOTHING;

-- Apelación rechazada para incidencia 6
INSERT INTO appeals (incidence_id, seller_id, reason, status, new_moderator_id, final_decision, final_decision_at)
VALUES
    (1,
     (SELECT id FROM users WHERE username = 'JoelB'),
     'Solicito revisión de mi caso. El bloqueo fue injusto ya que cumplí con todas las políticas.',
     'REVIEWED',
     (SELECT id FROM users WHERE username = 'moderator_three'),
     'REJECTED',
     NOW() - INTERVAL '2 days')
ON CONFLICT DO NOTHING;
