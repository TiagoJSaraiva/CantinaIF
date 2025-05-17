CREATE DATABASE todoapp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE todoapp;

-- Tabela do comprador (O usuário que realiza os pedidos)
CREATE TABLE Comprador (
    prontuario VARCHAR(10) PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    senha VARCHAR(100),
    nome VARCHAR(75)
);

-- Tabela do pedido realizado pelo Comprador
CREATE TABLE Pedido (
    id INT PRIMARY KEY AUTO_INCREMENT,
    comprador_prontuario VARCHAR(10),
    FOREIGN KEY (comprador_prontuario) REFERENCES Comprador(prontuario)
);

-- Tabela do produto (Os alimentos a venda)
CREATE TABLE Produto (
    id INT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(100),
    descricao VARCHAR(400),
    preco DECIMAL(10,2),
    estoque INT,
    categoria ENUM('salgado', 'doce', 'lanche', 'bebida')
);

-- Tabela do vendedor (O usuário que insere os pedidos no sistema)
CREATE TABLE Vendedor (
    prontuario VARCHAR(10) PRIMARY KEY,
    email VARCHAR(100) UNIQUE,
    senha VARCHAR(100)
);

-- Tabela intermediária para associar pedidos e produtos
CREATE TABLE Pedido_Produto (
    pedido_id INT,
    produto_id INT,
    quantidade INT DEFAULT 1,
    PRIMARY KEY (pedido_id, produto_id),
    FOREIGN KEY (pedido_id) REFERENCES Pedido(id),
    FOREIGN KEY (produto_id) REFERENCES Produto(id)
);

SELECT * FROM usuarios;
