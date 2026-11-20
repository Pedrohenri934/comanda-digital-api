-- Bebidas com escolha de sabor: refrigerante (Coca-Cola, Guarana, Sprite) e suco natural (manga, abacaxi, laranja).
-- Cada sabor e um prato "interno" (categoria inativa, nao aparece no cardapio) que da baixa no estoque certo.

-- ---------- Ingredientes ----------
UPDATE ingrediente SET nome = 'Coca-Cola lata 350ml' WHERE sku = 'ING-015';

INSERT INTO ingrediente (nome, sku, unidade_padrao, estoque_minimo, custo_unitario) VALUES
 ('Guaraná lata 350ml',      'ING-030', 'UN',   24, 3.2000),
 ('Sprite lata 350ml',       'ING-031', 'UN',   24, 3.2000),
 ('Manga',                   'ING-032', 'G',  2000, 0.0080),
 ('Abacaxi',                 'ING-033', 'G',  2000, 0.0060),
 ('Laranja',                 'ING-034', 'G',  3000, 0.0040),
 ('Embalagem de bebida',     'ING-035', 'UN',   50, 0.3000);

INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, lote, custo_unitario, usuario_id)
SELECT id, 'ENTRADA', CASE WHEN unidade_padrao = 'UN' THEN 150 ELSE 12000 END,
       'AJUSTE', 'LOTE-INICIAL', custo_unitario, 1
FROM ingrediente
WHERE sku IN ('ING-030','ING-031','ING-032','ING-033','ING-034','ING-035');

INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, lote, custo_unitario, usuario_id)
SELECT id, 'ENTRADA', 100, 'AJUSTE', 'LOTE-REPOSICAO', custo_unitario, 1 FROM ingrediente WHERE sku = 'ING-015';

-- ---------- Categoria interna e pratos de sabor ----------
INSERT INTO categoria (nome, descricao, ordem, status) VALUES ('Sabores (interno)', 'Sabores das bebidas, nao aparece no cardapio', 99, 'INATIVO');

INSERT INTO prato (categoria_id, nome, descricao, preco_venda, tempo_preparo_min, status)
SELECT c.id, v.nome, 'Sabor usado nas escolhas de bebida', v.preco, 2, 'ATIVO'
FROM categoria c
JOIN (
  SELECT 'Coca-Cola Lata 350ml' AS nome, 9.90 AS preco UNION ALL
  SELECT 'Guaraná Lata 350ml', 9.90 UNION ALL
  SELECT 'Sprite Lata 350ml', 9.90 UNION ALL
  SELECT 'Suco de Manga 400ml', 12.90 UNION ALL
  SELECT 'Suco de Abacaxi 400ml', 12.90 UNION ALL
  SELECT 'Suco de Laranja 400ml', 12.90
) v
WHERE c.nome = 'Sabores (interno)';

-- Prato novo do cardapio: Suco Natural (o cliente escolhe o sabor ao adicionar).
INSERT INTO prato (categoria_id, nome, descricao, preco_venda, tempo_preparo_min, status)
SELECT id, 'Suco Natural 400ml', 'Suco natural feito na hora. Escolha o sabor: manga, abacaxi ou laranja.', 12.90, 5, 'ATIVO'
FROM categoria WHERE nome = 'Bebidas' LIMIT 1;

UPDATE prato SET descricao = 'Refrigerante gelado. Escolha o sabor: Coca-Cola, Guaraná ou Sprite.' WHERE nome = 'Refrigerante Lata 350ml';

INSERT INTO cardapio_semana (dia_semana, prato_id, destaque)
SELECT d.n, p.id, FALSE
FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) d
JOIN prato p ON p.nome = 'Suco Natural 400ml';

-- ---------- Fichas tecnicas ----------
-- Os pratos "pai" (refrigerante e suco) ficam so com a embalagem; o sabor escolhido da baixa pela opcao.
DELETE fti FROM ficha_tecnica_item fti
JOIN ficha_tecnica ft ON ft.id = fti.ficha_tecnica_id
JOIN prato p ON p.id = ft.prato_id
WHERE p.nome = 'Refrigerante Lata 350ml';

INSERT INTO ficha_tecnica (prato_id, rendimento, modo_preparo)
SELECT id, 1, 'Servir gelado.' FROM prato
WHERE nome IN ('Coca-Cola Lata 350ml','Guaraná Lata 350ml','Sprite Lata 350ml','Suco de Manga 400ml',
               'Suco de Abacaxi 400ml','Suco de Laranja 400ml','Suco Natural 400ml')
  AND id NOT IN (SELECT prato_id FROM ficha_tecnica);

INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao)
SELECT ft.id, i.id, v.qtd, v.un, v.fc
FROM (
  SELECT 'Refrigerante Lata 350ml' AS prato, 'ING-035' AS sku, 1 AS qtd, 'UN' AS un, 1.00 AS fc UNION ALL
  SELECT 'Suco Natural 400ml', 'ING-035', 1, 'UN', 1.00 UNION ALL
  SELECT 'Coca-Cola Lata 350ml', 'ING-015', 1, 'UN', 1.00 UNION ALL
  SELECT 'Guaraná Lata 350ml', 'ING-030', 1, 'UN', 1.00 UNION ALL
  SELECT 'Sprite Lata 350ml', 'ING-031', 1, 'UN', 1.00 UNION ALL
  SELECT 'Suco de Manga 400ml', 'ING-032', 150, 'G', 1.30 UNION ALL
  SELECT 'Suco de Manga 400ml', 'ING-028', 15, 'G', 1.00 UNION ALL
  SELECT 'Suco de Abacaxi 400ml', 'ING-033', 200, 'G', 1.40 UNION ALL
  SELECT 'Suco de Abacaxi 400ml', 'ING-028', 15, 'G', 1.00 UNION ALL
  SELECT 'Suco de Laranja 400ml', 'ING-034', 400, 'G', 1.00 UNION ALL
  SELECT 'Suco de Laranja 400ml', 'ING-028', 10, 'G', 1.00
) v
JOIN prato p ON p.nome = v.prato
JOIN ficha_tecnica ft ON ft.prato_id = p.id
JOIN ingrediente i ON i.sku = v.sku;

-- ---------- Opcoes de escolha ----------
-- Combos: a bebida passa a ter refrigerantes, sucos e cerveja artesanal.
DELETE o FROM combo_opcao o
JOIN combo_grupo g ON g.id = o.grupo_id
JOIN prato p ON p.id = g.prato_id
WHERE p.nome IN ('Combo Boi Festão','Combo Família') AND g.nome IN ('Bebida','Bebidas');

-- Pratos simples com sabor.
INSERT INTO combo_grupo (prato_id, nome, minimo, maximo, ordem)
SELECT id, 'Sabor', 1, 1, 1 FROM prato WHERE nome IN ('Refrigerante Lata 350ml','Suco Natural 400ml');

INSERT INTO combo_opcao (grupo_id, nome, prato_id, acrescimo, ordem)
SELECT cg.id, o.opcao, pr.id, o.acr, o.ord
FROM (
  SELECT 'Combo Boi Festão' AS pai, 'Bebida' AS grupo, 'Coca-Cola' AS opcao, 'Coca-Cola Lata 350ml' AS prato_ref, 0.00 AS acr, 1 AS ord UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Guaraná', 'Guaraná Lata 350ml', 0.00, 2 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Sprite', 'Sprite Lata 350ml', 0.00, 3 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Suco de Manga', 'Suco de Manga 400ml', 3.00, 4 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Suco de Abacaxi', 'Suco de Abacaxi 400ml', 3.00, 5 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Suco de Laranja', 'Suco de Laranja 400ml', 3.00, 6 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Cerveja Artesanal', 'Cerveja Artesanal', 7.00, 7 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Coca-Cola', 'Coca-Cola Lata 350ml', 0.00, 1 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Guaraná', 'Guaraná Lata 350ml', 0.00, 2 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Sprite', 'Sprite Lata 350ml', 0.00, 3 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Suco de Manga', 'Suco de Manga 400ml', 3.00, 4 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Suco de Abacaxi', 'Suco de Abacaxi 400ml', 3.00, 5 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Suco de Laranja', 'Suco de Laranja 400ml', 3.00, 6 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Cerveja Artesanal', 'Cerveja Artesanal', 7.00, 7 UNION ALL
  SELECT 'Refrigerante Lata 350ml', 'Sabor', 'Coca-Cola', 'Coca-Cola Lata 350ml', 0.00, 1 UNION ALL
  SELECT 'Refrigerante Lata 350ml', 'Sabor', 'Guaraná', 'Guaraná Lata 350ml', 0.00, 2 UNION ALL
  SELECT 'Refrigerante Lata 350ml', 'Sabor', 'Sprite', 'Sprite Lata 350ml', 0.00, 3 UNION ALL
  SELECT 'Suco Natural 400ml', 'Sabor', 'Manga', 'Suco de Manga 400ml', 0.00, 1 UNION ALL
  SELECT 'Suco Natural 400ml', 'Sabor', 'Abacaxi', 'Suco de Abacaxi 400ml', 0.00, 2 UNION ALL
  SELECT 'Suco Natural 400ml', 'Sabor', 'Laranja', 'Suco de Laranja 400ml', 0.00, 3
) o
JOIN prato p ON p.nome = o.pai
JOIN combo_grupo cg ON cg.prato_id = p.id AND cg.nome = o.grupo
JOIN prato pr ON pr.nome = o.prato_ref;
