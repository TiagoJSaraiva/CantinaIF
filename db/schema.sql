-- ============================================================================
-- CantinaIF - esquema do banco de dados
--
-- Arquivo unico, compativel com MySQL 8 (8.0.16+) e com H2 em modo MySQL.
--   * MySQL: crie o banco manualmente e aplique este arquivo, por exemplo:
--       CREATE DATABASE cantinaif CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
--       mysql cantinaif < db/schema.sql
--   * H2 (padrao quando CANTINA_DB_URL nao esta definida): aplicado
--     automaticamente no boot da aplicacao (utils.Database).
--
-- Restaurado do historico do repositorio (CantinaDB.sql) e ajustado ao que os
-- DAOs realmente usam. Diferencas em relacao ao original:
--   * sem CREATE DATABASE/USE (o banco e escolhido pela URL de conexao);
--   * ENUM substituido por VARCHAR + CHECK (portavel entre MySQL e H2); a
--     aplicacao grava a categoria sempre em minusculas;
--   * tabelas criadas com IF NOT EXISTS, colunas essenciais NOT NULL e CHECKs
--     de preco/estoque/quantidade nao negativos;
--   * o INSERT do vendedor ficticio (senha em texto puro) foi removido: senhas
--     agora sao guardadas como hash BCrypt (60 caracteres, cabem em VARCHAR(100)).
--
-- CONTA DE DEMONSTRACAO (nao e segredo): em modo H2 a aplicacao cria no boot,
-- se nao existir, o vendedor
--     prontuario: VEND001   e-mail: vendedor@cantinaif.demo   senha: demo1234
-- A senha e gravada como hash BCrypt pela aplicacao (utils.Database).
-- ============================================================================

-- Comprador (aluno ou colaborador que faz os pedidos)
CREATE TABLE IF NOT EXISTS Comprador (
    prontuario VARCHAR(10)  NOT NULL PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    senha      VARCHAR(100) NOT NULL,           -- hash BCrypt
    nome       VARCHAR(75)
);

-- Vendedor (responsavel pela cantina)
CREATE TABLE IF NOT EXISTS Vendedor (
    prontuario VARCHAR(10)  NOT NULL PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    senha      VARCHAR(100) NOT NULL            -- hash BCrypt
);

-- Produto (itens a venda)
CREATE TABLE IF NOT EXISTS Produto (
    id        INT           NOT NULL PRIMARY KEY AUTO_INCREMENT,
    nome      VARCHAR(100)  NOT NULL,
    descricao VARCHAR(400),
    preco     DECIMAL(10,2) NOT NULL,
    estoque   INT           NOT NULL DEFAULT 0,
    categoria VARCHAR(10),
    imagem    MEDIUMBLOB,
    CONSTRAINT ck_produto_preco     CHECK (preco >= 0),
    CONSTRAINT ck_produto_estoque   CHECK (estoque >= 0),
    CONSTRAINT ck_produto_categoria CHECK (categoria IN ('salgado', 'doce', 'lanche', 'bebida'))
);

-- Pedido realizado por um comprador
CREATE TABLE IF NOT EXISTS Pedido (
    id                   INT         NOT NULL PRIMARY KEY AUTO_INCREMENT,
    comprador_prontuario VARCHAR(10) NOT NULL,
    CONSTRAINT fk_pedido_comprador FOREIGN KEY (comprador_prontuario) REFERENCES Comprador(prontuario)
);

-- Itens do pedido (um produto aparece no maximo uma vez por pedido)
CREATE TABLE IF NOT EXISTS Pedido_Produto (
    pedido_id  INT NOT NULL,
    produto_id INT NOT NULL,
    quantidade INT NOT NULL DEFAULT 1,
    PRIMARY KEY (pedido_id, produto_id),
    CONSTRAINT fk_pp_pedido  FOREIGN KEY (pedido_id)  REFERENCES Pedido(id),
    CONSTRAINT fk_pp_produto FOREIGN KEY (produto_id) REFERENCES Produto(id),
    CONSTRAINT ck_pp_quantidade CHECK (quantidade >= 1)
);
