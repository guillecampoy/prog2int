CREATE DATABASE IF NOT EXISTS db_historiaclinica;
USE db_historiaclinica;

CREATE TABLE paciente (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    dni VARCHAR(15) NOT NULL UNIQUE,
    fecha_nacimiento DATE NOT NULL,
    eliminado BOOLEAN DEFAULT FALSE
);

CREATE TABLE historias_clinicas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nro_historia VARCHAR(20) NOT NULL UNIQUE,
    paciente_id INT NOT NULL,
    grupo_sanguineo VARCHAR(3) NOT NULL,
    antecedentes TEXT NULL,
    medicacion_actual TEXT NULL,
    observaciones TEXT NULL,
    eliminado BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_historia_paciente FOREIGN KEY (paciente_id) REFERENCES paciente(id),
    CONSTRAINT uk_historia_paciente UNIQUE (paciente_id)
);