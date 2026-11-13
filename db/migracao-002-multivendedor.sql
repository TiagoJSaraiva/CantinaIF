-- ============================================================================
-- CantinaIF - migracao 002: multivendedor (MySQL 8)
--
-- Para bancos MySQL criados com o esquema anterior (um unico vendedor). Bancos novos e o H2 da demonstracao ja
-- nascem com o esquema atual (db/schema.sql) e NAO precisam deste script.
--
-- O que muda:
--   * Vendedor ganha nome (nome da cantina, mostrado aos compradores);
--   * Comprador ganha vendedor vinculado (NULL = sem vinculo, nao ve produtos) e situacao (ativo);
--   * Produto e Pedido passam a pertencer a um vendedor;
--   * Pedido registra quem cancelou e o motivo informado pelo vendedor;
--   * novas tabelas CodigoVinculo (codigo de 60 s, uso unico) e Advertencia.
--
-- Os dados existentes sao atribuidos ao vendedor informado em @vendedor (ajuste antes de rodar). Todos os
-- compradores existentes ficam vinculados a ele, e os pedidos cancelados antigos ficam com cancelado_por NULL.
--
--   mysql cantinaif < db/migracao-002-multivendedor.sql
-- Faca um backup antes.
-- ============================================================================

SET @vendedor = (SELECT prontuario FROM Vendedor ORDER BY prontuario LIMIT 1);

ALTER TABLE Vendedor ADD COLUMN nome VARCHAR(75) NULL;
UPDATE Vendedor SET nome = CONCAT('Cantina ', prontuario) WHERE nome IS NULL;
ALTER TABLE Vendedor MODIFY nome VARCHAR(75) NOT NULL;

ALTER TABLE Comprador
    ADD COLUMN vendedor_prontuario VARCHAR(10) NULL,
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT fk_comprador_vendedor FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario);
UPDATE Comprador SET vendedor_prontuario = @vendedor;

ALTER TABLE Produto ADD COLUMN vendedor_prontuario VARCHAR(10) NULL;
UPDATE Produto SET vendedor_prontuario = @vendedor;
ALTER TABLE Produto
    MODIFY vendedor_prontuario VARCHAR(10) NOT NULL,
    ADD CONSTRAINT fk_produto_vendedor FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario);

ALTER TABLE Pedido
    ADD COLUMN vendedor_prontuario VARCHAR(10) NULL,
    ADD COLUMN cancelado_por VARCHAR(10) NULL,
    ADD COLUMN motivo_cancelamento VARCHAR(300) NULL;
UPDATE Pedido SET vendedor_prontuario = @vendedor;
ALTER TABLE Pedido
    MODIFY vendedor_prontuario VARCHAR(10) NOT NULL,
    ADD CONSTRAINT ck_pedido_cancelado_por CHECK (cancelado_por IS NULL OR cancelado_por IN ('COMPRADOR', 'VENDEDOR')),
    ADD CONSTRAINT fk_pedido_vendedor FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario);

CREATE TABLE IF NOT EXISTS CodigoVinculo (
    codigo              VARCHAR(8)  NOT NULL PRIMARY KEY,
    vendedor_prontuario VARCHAR(10) NOT NULL,
    criado_em           TIMESTAMP   NOT NULL,
    expira_em           TIMESTAMP   NOT NULL,
    usado_por           VARCHAR(10),
    usado_em            TIMESTAMP   NULL,
    CONSTRAINT fk_cv_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario),
    CONSTRAINT fk_cv_comprador FOREIGN KEY (usado_por) REFERENCES Comprador(prontuario)
);

CREATE TABLE IF NOT EXISTS Advertencia (
    id                   INT          NOT NULL PRIMARY KEY AUTO_INCREMENT,
    comprador_prontuario VARCHAR(10)  NOT NULL,
    vendedor_prontuario  VARCHAR(10)  NOT NULL,
    mensagem             VARCHAR(500) NOT NULL,
    criada_em            TIMESTAMP    NOT NULL,
    lida_em              TIMESTAMP    NULL,
    CONSTRAINT fk_adv_comprador FOREIGN KEY (comprador_prontuario) REFERENCES Comprador(prontuario),
    CONSTRAINT fk_adv_vendedor  FOREIGN KEY (vendedor_prontuario) REFERENCES Vendedor(prontuario)
);
