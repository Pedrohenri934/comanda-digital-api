-- Dados iniciais. Admin: admin@email.com / senha123 (hash BCrypt, nunca a senha em texto).

INSERT INTO usuario (id, nome, email, senha_hash, perfil, status)
VALUES (1, 'Administrador', 'admin@email.com', '$2a$10$uhiLcVWdQuId8.0ARaXg0.xt8uyRRQ7TvNV4cLQmfT4dUxvytzKBC', 'ADMIN', 'ATIVO');

INSERT INTO categoria (id, nome, descricao, ordem) VALUES
 (1, 'Lanches',  'Hamburgueres e sanduiches artesanais', 1),
 (2, 'Acai',     'Acai e complementos',                  2),
 (3, 'Porcoes',  'Acompanhamentos para dividir',         3),
 (4, 'Bebidas',  'Refrigerantes e sucos',                4);

INSERT INTO ingrediente (id, nome, sku, unidade_padrao, estoque_minimo, custo_unitario) VALUES
 (1,  'Blend bovino',          'ING-001', 'G',  2000, 0.0450),
 (2,  'Pao brioche',           'ING-002', 'UN',   20, 2.8000),
 (3,  'Queijo cheddar',        'ING-003', 'G',  1000, 0.0650),
 (4,  'Alface',                'ING-004', 'G',   500, 0.0120),
 (5,  'Tomate',                'ING-005', 'G',   500, 0.0080),
 (6,  'Molho especial',        'ING-006', 'ML', 1000, 0.0320),
 (7,  'Frango empanado',       'ING-007', 'G',  2000, 0.0300),
 (8,  'Polpa de acai',         'ING-008', 'G',  3000, 0.0220),
 (9,  'Granola',               'ING-009', 'G',   500, 0.0180),
 (10, 'Leite condensado',      'ING-010', 'G',   500, 0.0250),
 (11, 'Banana',                'ING-011', 'G',  1000, 0.0060),
 (12, 'Batata congelada',      'ING-012', 'G',  3000, 0.0070),
 (13, 'Oleo de fritura',       'ING-013', 'ML', 2000, 0.0090),
 (14, 'Bacon em cubos',        'ING-014', 'G',   500, 0.0500),
 (15, 'Refrigerante lata 350ml','ING-015','UN',   24, 3.2000);

INSERT INTO prato (id, categoria_id, nome, descricao, preco_venda, tempo_preparo_min, status) VALUES
 (1, 1, 'Hamburguer Artesanal',   'Blend bovino 180g, cheddar, alface, tomate e molho especial no pao brioche.', 39.90, 18, 'ATIVO'),
 (2, 1, 'X-Frango Crocante',      'Frango empanado, cheddar, alface e molho especial no pao brioche.',           32.90, 15, 'ATIVO'),
 (3, 2, 'Acai 500ml',             'Acai batido com banana, granola e leite condensado.',                         24.90,  8, 'ATIVO'),
 (4, 3, 'Batata Frita Rustica',   'Batata rustica com cheddar e bacon.',                                         19.90, 12, 'ATIVO'),
 (5, 4, 'Refrigerante Lata 350ml','Refrigerante gelado.',                                                          9.90,  2, 'ATIVO');

INSERT INTO ficha_tecnica (id, prato_id, rendimento, modo_preparo) VALUES
 (1, 1, 1, '1) Grelhar o blend 4 min de cada lado. 2) Tostar o pao. 3) Montar com queijo, alface, tomate e molho.'),
 (2, 2, 1, '1) Fritar o frango ate dourar. 2) Tostar o pao. 3) Montar com queijo, alface e molho.'),
 (3, 3, 1, '1) Bater a polpa com a banana. 2) Servir no copo com granola e leite condensado.'),
 (4, 4, 1, '1) Fritar a batata em oleo quente. 2) Servir com cheddar e bacon crocante.'),
 (5, 5, 1, 'Servir gelado.');

INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao) VALUES
 (1, 1, 180, 'G',  1.00),
 (1, 2,   1, 'UN', 1.00),
 (1, 3,  40, 'G',  1.00),
 (1, 4,  30, 'G',  1.40),
 (1, 5,  50, 'G',  1.25),
 (1, 6,  25, 'ML', 1.00),
 (2, 7, 150, 'G',  1.00),
 (2, 2,   1, 'UN', 1.00),
 (2, 3,  30, 'G',  1.00),
 (2, 4,  30, 'G',  1.40),
 (2, 6,  25, 'ML', 1.00),
 (3, 8, 300, 'G',  1.00),
 (3, 9,  30, 'G',  1.00),
 (3, 10, 20, 'G',  1.00),
 (3, 11, 80, 'G',  1.30),
 (4, 12,250, 'G',  1.15),
 (4, 13, 40, 'ML', 1.00),
 (4, 3,  30, 'G',  1.00),
 (4, 14, 20, 'G',  1.00),
 (5, 15,  1, 'UN', 1.00);

-- Estoque inicial (entrada de ajuste feita pelo admin). O bacon fica abaixo do minimo de proposito, para o alerta aparecer.
INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, lote, custo_unitario, usuario_id)
SELECT id, 'ENTRADA',
       CASE WHEN sku = 'ING-014' THEN 300
            WHEN unidade_padrao = 'UN' THEN 100
            WHEN unidade_padrao = 'ML' THEN 5000
            ELSE 10000 END,
       'AJUSTE', 'LOTE-INICIAL', custo_unitario, 1
FROM ingrediente;
