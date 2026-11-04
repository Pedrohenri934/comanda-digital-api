-- Comanda Digital - schema inicial (MySQL 8+). Nomes em snake_case.

CREATE TABLE usuario (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL UNIQUE,
    senha_hash    VARCHAR(100) NOT NULL,
    perfil        VARCHAR(20)  NOT NULL,                 -- ADMIN / GERENTE / COZINHEIRO / CLIENTE
    telefone      VARCHAR(20),
    endereco      VARCHAR(255),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ATIVO', -- ATIVO / INATIVO (exclusao logica, RN06)
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE categoria (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(80)  NOT NULL,
    descricao   VARCHAR(255),
    ordem       INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE prato (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    categoria_id       BIGINT        NOT NULL,
    nome               VARCHAR(120)  NOT NULL,
    descricao          TEXT,
    foto_url           VARCHAR(500),
    preco_venda        DECIMAL(10,2) NOT NULL,
    tempo_preparo_min  INT           NOT NULL DEFAULT 15,
    status             VARCHAR(20)   NOT NULL DEFAULT 'INATIVO', -- ATIVO / INATIVO / PAUSADO
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prato_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id)
);

CREATE TABLE ingrediente (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(120)  NOT NULL,
    sku             VARCHAR(40)   NOT NULL UNIQUE,
    unidade_padrao  VARCHAR(5)    NOT NULL,                  -- G / ML / UN / KG / L
    estoque_minimo  DECIMAL(12,3) NOT NULL DEFAULT 0,
    custo_unitario  DECIMAL(10,4) NOT NULL DEFAULT 0,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ATIVO',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ficha_tecnica (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    prato_id      BIGINT NOT NULL UNIQUE,
    rendimento    INT    NOT NULL DEFAULT 1,
    modo_preparo  TEXT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ficha_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

CREATE TABLE ficha_tecnica_item (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    ficha_tecnica_id  BIGINT        NOT NULL,
    ingrediente_id    BIGINT        NOT NULL,
    quantidade        DECIMAL(12,3) NOT NULL,
    unidade           VARCHAR(5)    NOT NULL,
    fator_correcao    DECIMAL(5,2)  NOT NULL DEFAULT 1.00,   -- RN08: >= 1.0
    CONSTRAINT fk_fti_ficha FOREIGN KEY (ficha_tecnica_id) REFERENCES ficha_tecnica (id),
    CONSTRAINT fk_fti_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id)
);

CREATE TABLE fornecedor (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    razao_social  VARCHAR(160) NOT NULL,
    cnpj          VARCHAR(18)  NOT NULL UNIQUE,
    telefone      VARCHAR(20),
    email         VARCHAR(160),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ATIVO',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fornecedor_produto (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    fornecedor_id   BIGINT        NOT NULL,
    ingrediente_id  BIGINT        NOT NULL,
    preco           DECIMAL(10,4) NOT NULL,
    unidade_venda   VARCHAR(10)   NOT NULL,
    CONSTRAINT uq_fornecedor_ingrediente UNIQUE (fornecedor_id, ingrediente_id),
    CONSTRAINT fk_fp_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id),
    CONSTRAINT fk_fp_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id)
);

CREATE TABLE pedido_compra (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    fornecedor_id  BIGINT        NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'RASCUNHO', -- RASCUNHO / ENVIADO / RECEBIDO / CANCELADO
    valor_total    DECIMAL(12,2) NOT NULL DEFAULT 0,
    usuario_id     BIGINT        NOT NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pc_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id),
    CONSTRAINT fk_pc_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE TABLE pedido_compra_item (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_compra_id BIGINT        NOT NULL,
    ingrediente_id   BIGINT        NOT NULL,
    quantidade       DECIMAL(12,3) NOT NULL,
    preco_unitario   DECIMAL(10,4) NOT NULL,
    subtotal         DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_pci_pedido FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra (id),
    CONSTRAINT fk_pci_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id)
);

CREATE TABLE pedido (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id          BIGINT        NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'RECEBIDO',
    valor_total         DECIMAL(10,2) NOT NULL,
    endereco_entrega    VARCHAR(255)  NOT NULL,
    observacoes         VARCHAR(500),
    motivo_cancelamento VARCHAR(255),
    canal               VARCHAR(20)   NOT NULL DEFAULT 'SITE', -- extra: RF-019 pede filtro por canal
    pago                BOOLEAN       NOT NULL DEFAULT FALSE,  -- extra: RF-006 pagamento simulado
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES usuario (id)
);

CREATE TABLE pedido_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       BIGINT        NOT NULL,
    prato_id        BIGINT        NOT NULL,
    quantidade      INT           NOT NULL,
    preco_unitario  DECIMAL(10,2) NOT NULL,
    observacoes     VARCHAR(255),
    CONSTRAINT fk_pi_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_pi_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

CREATE TABLE estoque_movimentacao (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    ingrediente_id    BIGINT        NOT NULL,
    tipo              VARCHAR(10)   NOT NULL,                 -- ENTRADA / SAIDA / ESTORNO
    quantidade        DECIMAL(12,3) NOT NULL,
    motivo            VARCHAR(20)   NOT NULL,                 -- COMPRA / VENDA / DESPERDICIO / VENCIMENTO / QUEBRA / USO_INTERNO / AJUSTE / ESTORNO
    lote              VARCHAR(50),
    validade          DATE,
    custo_unitario    DECIMAL(10,4),
    pedido_compra_id  BIGINT NULL,
    pedido_id         BIGINT NULL,
    usuario_id        BIGINT NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_em_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id),
    CONSTRAINT fk_em_pedido_compra FOREIGN KEY (pedido_compra_id) REFERENCES pedido_compra (id),
    CONSTRAINT fk_em_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id),
    CONSTRAINT fk_em_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

CREATE INDEX idx_pedido_cliente ON pedido (cliente_id);
CREATE INDEX idx_pedido_status ON pedido (status);
CREATE INDEX idx_em_ingrediente ON estoque_movimentacao (ingrediente_id);
