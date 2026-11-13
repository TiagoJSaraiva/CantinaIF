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
--   * Pedido ganhou status, criado_em e total; Pedido_Produto guarda preco_unitario; Comprador ganhou cpf.
--     (Bancos MySQL criados com a versao anterior precisam de ALTER TABLE equivalente.)
--   * o INSERT do vendedor ficticio (senha em texto puro) foi removido: senhas
--     agora sao guardadas como hash BCrypt (60 caracteres, cabem em VARCHAR(100)).
--
-- CONTAS DE DEMONSTRACAO (nao sao segredos): em modo H2 a aplicacao semeia dados ficticios no boot
-- (seed.DemoDataSeeder: 30 produtos, 19 compradores, 50 pedidos). Senha das duas contas: demo1234
--     vendedor:  prontuario VEND001     e-mail vendedor@cantinaif.demo
--     comprador: prontuario CJ3000001   e-mail aluno@cantinaif.demo
-- As senhas sao gravadas como hash BCrypt pela aplicacao. Para recriar os dados em outro banco:
--     ./mvnw -q compile exec:java -Dexec.args=--confirmar-apagar   (apaga os dados existentes!)
-- ============================================================================

-- Vendedor (dono de uma cantina/lanchonete; cada um tem os proprios produtos, pedidos e compradores)
CREATE TABLE IF NOT EXISTS Vendedor (
    prontuario VARCHAR(10)  NOT NULL PRIMARY KEY,
    email      VARCHAR(100) NOT NULL UNIQUE,
    senha      VARCHAR(100) NOT NULL,           -- hash BCrypt
    nome       VARCHAR(75)  NOT NULL            -- nome da cantina, mostrado aos compradores
);

-- Comprador (aluno ou colaborador que faz os pedidos). Fica vinculado a no maximo UM vendedor (por um codigo
-- temporario fornecido pelo vendedor) e so ve os produtos desse vendedor; sem vinculo nao ve produto nenhum.
CREATE TABLE IF NOT EXISTS Comprador (
    prontuario          VARCHAR(10)  NOT NULL PRIMARY KEY,
    email               VARCHAR(100) NOT NULL UNIQUE,
    senha               VARCHAR(100) NOT NULL,           -- hash BCrypt
    nome                VARCHAR(75),
    cpf                 VARCHAR(11) UNIQUE,              -- opcional; nos dados de demonstracao e sempre ficticio
    vendedor_prontuario VARCHAR(10),                     -- vendedor vinculado (NULL = sem vinculo)
    ativo               BOOLEAN      NOT NULL DEFAULT TRUE, -- FALSE = desativado pelo vendedor (sem acesso)
    CONSTRAINT fk_comprador_vendedor FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario)
);

-- Produto (itens a venda de um vendedor)
CREATE TABLE IF NOT EXISTS Produto (
    id                  INT           NOT NULL PRIMARY KEY AUTO_INCREMENT,
    vendedor_prontuario VARCHAR(10)   NOT NULL,
    nome                VARCHAR(100)  NOT NULL,
    descricao           VARCHAR(400),
    preco               DECIMAL(10,2) NOT NULL,
    estoque             INT           NOT NULL DEFAULT 0,
    categoria           VARCHAR(10),
    imagem              MEDIUMBLOB,
    CONSTRAINT ck_produto_preco     CHECK (preco >= 0),
    CONSTRAINT ck_produto_estoque   CHECK (estoque >= 0),
    CONSTRAINT ck_produto_categoria CHECK (categoria IN ('salgado', 'doce', 'lanche', 'bebida')),
    CONSTRAINT fk_produto_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario)
);

-- Pedido realizado por um comprador a um vendedor
CREATE TABLE IF NOT EXISTS Pedido (
    id                   INT           NOT NULL PRIMARY KEY AUTO_INCREMENT,
    comprador_prontuario VARCHAR(10)   NOT NULL,
    vendedor_prontuario  VARCHAR(10)   NOT NULL,
    status               VARCHAR(12)   NOT NULL DEFAULT 'PENDENTE',
    criado_em            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total                DECIMAL(10,2) NOT NULL DEFAULT 0,   -- calculado no servidor
    cancelado_por        VARCHAR(10),                        -- COMPRADOR ou VENDEDOR (so quando CANCELADO)
    motivo_cancelamento  VARCHAR(300),                       -- obrigatorio quando o vendedor cancela
    CONSTRAINT ck_pedido_status CHECK (status IN ('PENDENTE', 'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO')),
    CONSTRAINT ck_pedido_total  CHECK (total >= 0),
    CONSTRAINT ck_pedido_cancelado_por CHECK (cancelado_por IS NULL OR cancelado_por IN ('COMPRADOR', 'VENDEDOR')),
    CONSTRAINT fk_pedido_comprador FOREIGN KEY (comprador_prontuario) REFERENCES Comprador(prontuario),
    CONSTRAINT fk_pedido_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario)
);

-- Itens do pedido (um produto aparece no maximo uma vez por pedido)
CREATE TABLE IF NOT EXISTS Pedido_Produto (
    pedido_id      INT           NOT NULL,
    produto_id     INT           NOT NULL,
    quantidade     INT           NOT NULL DEFAULT 1,
    preco_unitario DECIMAL(10,2) NOT NULL DEFAULT 0,   -- preco do produto no momento da compra
    PRIMARY KEY (pedido_id, produto_id),
    CONSTRAINT fk_pp_pedido  FOREIGN KEY (pedido_id)  REFERENCES Pedido(id),
    CONSTRAINT fk_pp_produto FOREIGN KEY (produto_id) REFERENCES Produto(id),
    CONSTRAINT ck_pp_quantidade CHECK (quantidade >= 1)
);

-- Codigo de vinculo: gerado pelo vendedor, vale 60 segundos e uma unica vez (liga um comprador ao vendedor).
CREATE TABLE IF NOT EXISTS CodigoVinculo (
    codigo              VARCHAR(8)  NOT NULL PRIMARY KEY,
    vendedor_prontuario VARCHAR(10) NOT NULL,
    criado_em           TIMESTAMP   NOT NULL,
    expira_em           TIMESTAMP   NOT NULL,
    usado_por           VARCHAR(10),                 -- comprador que usou (NULL = ainda nao usado)
    usado_em            TIMESTAMP,
    CONSTRAINT fk_cv_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario),
    CONSTRAINT fk_cv_comprador FOREIGN KEY (usado_por) REFERENCES Comprador(prontuario)
);

-- Advertencia enviada por um vendedor a um comprador vinculado (aparece para o comprador ate ser lida).
CREATE TABLE IF NOT EXISTS Advertencia (
    id                   INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
    comprador_prontuario VARCHAR(10)  NOT NULL,
    vendedor_prontuario  VARCHAR(10)  NOT NULL,
    mensagem             VARCHAR(500) NOT NULL,
    criada_em            TIMESTAMP    NOT NULL,
    lida_em              TIMESTAMP,
    CONSTRAINT fk_adv_comprador FOREIGN KEY (comprador_prontuario) REFERENCES Comprador(prontuario),
    CONSTRAINT fk_adv_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario)
);
