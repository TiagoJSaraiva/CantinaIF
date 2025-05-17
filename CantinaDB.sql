CREATE DATABASE todoapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE todoapp;

CREATE TABLE usuarios (
    prontuario VARCHAR(10) PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    senha VARCHAR(100),
    nome VARCHAR(75)
);

SELECT * FROM usuarios;
