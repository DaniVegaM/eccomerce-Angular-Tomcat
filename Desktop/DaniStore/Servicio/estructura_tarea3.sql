-- Script para crear y modificar las tablas necesarias para el sistema de comercio electrónico

-- Tabla usuarios (agrega password y token si no existen)
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INTEGER AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100),
    fecha_nacimiento DATETIME NOT NULL,
    telefono BIGINT,
    genero CHAR(1),
    password VARCHAR(20),
    token VARCHAR(20)
);

CREATE UNIQUE INDEX IF NOT EXISTS usuarios_1 ON usuarios(email);

-- Si la tabla usuarios ya existe, agrega las columnas password y token solo si no existen
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_NAME='usuarios' AND COLUMN_NAME='password');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE usuarios ADD COLUMN password VARCHAR(20);', 'SELECT 1;');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
  WHERE TABLE_NAME='usuarios' AND COLUMN_NAME='token');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE usuarios ADD COLUMN token VARCHAR(20);', 'SELECT 1;');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Tabla fotos_usuarios
CREATE TABLE IF NOT EXISTS fotos_usuarios (
    id_foto INTEGER AUTO_INCREMENT PRIMARY KEY,
    foto LONGBLOB,
    id_usuario INTEGER NOT NULL,
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);

-- Tabla stock
CREATE TABLE IF NOT EXISTS stock (
    id_articulo INTEGER AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    precio DOUBLE NOT NULL,
    cantidad INTEGER NOT NULL
);

-- Tabla fotos_articulos
CREATE TABLE IF NOT EXISTS fotos_articulos (
    id_foto INTEGER AUTO_INCREMENT PRIMARY KEY,
    foto LONGBLOB,
    id_articulo INTEGER NOT NULL,
    FOREIGN KEY (id_articulo) REFERENCES stock(id_articulo)
);

-- Tabla carrito_compra
CREATE TABLE IF NOT EXISTS carrito_compra (
    id_usuario INTEGER NOT NULL,
    id_articulo INTEGER NOT NULL,
    cantidad INTEGER NOT NULL,
    PRIMARY KEY (id_usuario, id_articulo),
    FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario),
    FOREIGN KEY (id_articulo) REFERENCES stock(id_articulo)
);
