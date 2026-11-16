-- Entregadores: cadastro com aprovacao do admin e vinculo do pedido com quem entrega.
CREATE TABLE entregador (
    usuario_id       BIGINT      PRIMARY KEY,
    veiculo          VARCHAR(30) NOT NULL,
    placa            VARCHAR(10),
    status_cadastro  VARCHAR(12) NOT NULL DEFAULT 'PENDENTE',  -- PENDENTE / APROVADO / RECUSADO
    disponivel       BOOLEAN     NOT NULL DEFAULT FALSE,       -- "online" para receber entregas
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entregador_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
);

ALTER TABLE pedido ADD COLUMN entregador_id BIGINT NULL;
ALTER TABLE pedido ADD CONSTRAINT fk_pedido_entregador FOREIGN KEY (entregador_id) REFERENCES usuario (id);

-- Contas de teste (senha 123456): dois entregadores aprovados e um pendente para testar a aprovacao.
INSERT INTO usuario (nome, email, senha_hash, perfil, telefone, status) VALUES
 ('Carlos Moto',   'carlos@entrega.com', '$2a$10$s9HyjvdIIVqu5sC.Fp2CfePWT8GIV6O4ZfcROcFennkHiBSImArXu', 'ENTREGADOR', '(11) 99999-0001', 'ATIVO'),
 ('Ana Bike',      'ana@entrega.com',    '$2a$10$s9HyjvdIIVqu5sC.Fp2CfePWT8GIV6O4ZfcROcFennkHiBSImArXu', 'ENTREGADOR', '(11) 99999-0002', 'ATIVO'),
 ('João Pendente', 'joao@entrega.com',   '$2a$10$s9HyjvdIIVqu5sC.Fp2CfePWT8GIV6O4ZfcROcFennkHiBSImArXu', 'ENTREGADOR', '(11) 99999-0003', 'ATIVO');

INSERT INTO entregador (usuario_id, veiculo, placa, status_cadastro)
SELECT id, 'Moto', 'ABC-1234', 'APROVADO' FROM usuario WHERE email = 'carlos@entrega.com';
INSERT INTO entregador (usuario_id, veiculo, placa, status_cadastro)
SELECT id, 'Bicicleta', NULL, 'APROVADO' FROM usuario WHERE email = 'ana@entrega.com';
INSERT INTO entregador (usuario_id, veiculo, placa, status_cadastro)
SELECT id, 'Moto', 'XYZ-9876', 'PENDENTE' FROM usuario WHERE email = 'joao@entrega.com';
