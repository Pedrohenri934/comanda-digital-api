-- Boi Na Brasa: categorias de churrasco, pratos com foto, fichas tecnicas e estoque inicial.
-- Nao usa IDs fixos (busca por nome/sku), entao funciona mesmo se o banco ja tiver dados cadastrados na mao.

-- ---------- Categorias ----------
UPDATE categoria SET nome = 'Sobremesas', descricao = 'Doces e sobremesas' WHERE nome = 'Acai';
UPDATE categoria SET nome = 'Porções', descricao = 'Acompanhamentos para dividir' WHERE nome = 'Porcoes';

INSERT INTO categoria (nome, descricao, ordem) VALUES
 ('Carnes', 'Cortes grelhados na brasa', 0),
 ('Combos', 'Combos para matar a fome', 6);

UPDATE categoria SET ordem = 1 WHERE nome = 'Carnes';
UPDATE categoria SET ordem = 2 WHERE nome = 'Porções';
UPDATE categoria SET ordem = 3 WHERE nome = 'Lanches';
UPDATE categoria SET ordem = 4 WHERE nome = 'Sobremesas';
UPDATE categoria SET ordem = 5 WHERE nome = 'Bebidas';
UPDATE categoria SET ordem = 6 WHERE nome = 'Combos';

-- ---------- Pratos que ja existiam: acentos e fotos ----------
UPDATE prato SET nome = 'Hambúrguer Artesanal',
  foto_url = 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&q=80'
  WHERE nome = 'Hamburguer Artesanal';
UPDATE prato SET nome = 'Açaí 500ml',
  foto_url = 'https://images.unsplash.com/photo-1590301157890-4810ed352733?w=600&q=80'
  WHERE nome = 'Acai 500ml';
UPDATE prato SET nome = 'Batata Frita Rústica',
  foto_url = 'https://images.unsplash.com/photo-1762922425286-5c3dabdb2d83?w=600&q=80'
  WHERE nome = 'Batata Frita Rustica';
UPDATE prato SET foto_url = 'https://images.unsplash.com/photo-1763297059500-5810c9142d2c?w=600&q=80'
  WHERE nome = 'Refrigerante Lata 350ml';

-- ---------- Ingredientes novos ----------
INSERT INTO ingrediente (nome, sku, unidade_padrao, estoque_minimo, custo_unitario) VALUES
 ('Picanha',               'ING-016', 'G',  2000, 0.0600),
 ('Costela bovina',        'ING-017', 'G',  3000, 0.0420),
 ('Filé mignon',           'ING-018', 'G',  2000, 0.0800),
 ('Linguiça toscana',      'ING-019', 'G',  2000, 0.0300),
 ('Coração de frango',     'ING-020', 'G',  1500, 0.0200),
 ('Alho',                  'ING-021', 'G',   300, 0.0200),
 ('Mandioca',              'ING-022', 'G',  3000, 0.0050),
 ('Manteiga',              'ING-023', 'G',   500, 0.0400),
 ('Ervas finas',           'ING-024', 'G',   100, 0.1000),
 ('Cerveja artesanal 350ml','ING-025','UN',   24, 6.0000),
 ('Leite integral',        'ING-026', 'ML', 2000, 0.0050),
 ('Ovos',                  'ING-027', 'UN',   30, 0.8000),
 ('Açúcar',                'ING-028', 'G',  1000, 0.0050),
 ('Tempero da casa',       'ING-029', 'G',   200, 0.0300);

-- ---------- Pratos novos (nascem ATIVOS; a ficha tecnica entra logo abaixo) ----------
INSERT INTO prato (categoria_id, nome, descricao, foto_url, preco_venda, tempo_preparo_min, status) VALUES
 ((SELECT id FROM categoria WHERE nome = 'Carnes' LIMIT 1), 'Picanha na Brasa',
  'Picanha premium grelhada no carvão, servida com alho frito',
  'https://images.unsplash.com/photo-1596995804697-27d11d43652e?w=600&q=80', 89.90, 25, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Carnes' LIMIT 1), 'Costela Bovina',
  'Costela bovina defumada por 12 horas, desfiada na hora',
  'https://images.unsplash.com/photo-1720569594980-0b7a5450d989?w=600&q=80', 79.90, 30, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Carnes' LIMIT 1), 'Filé Mignon Grelhado',
  'Filé mignon grelhado ao ponto, com molho de ervas finas',
  'https://images.unsplash.com/photo-1709433420549-06e96379c2b9?w=600&q=80', 99.90, 22, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Carnes' LIMIT 1), 'Linguiça Toscana',
  'Linguiça toscana artesanal grelhada na brasa com pimenta',
  'https://images.unsplash.com/photo-1703355333430-4c799ed364be?w=600&q=80', 44.90, 15, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Carnes' LIMIT 1), 'Coração de Frango',
  'Corações de frango temperados e grelhados no espeto',
  'https://images.unsplash.com/photo-1703355333430-4c799ed364be?w=600&q=80', 34.90, 15, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Porções' LIMIT 1), 'Fritas Crocantes',
  'Batata frita crocante com tempero especial da casa',
  'https://images.unsplash.com/photo-1762922425286-5c3dabdb2d83?w=600&q=80', 24.90, 12, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Porções' LIMIT 1), 'Purê de Mandioca',
  'Purê cremoso de mandioca com manteiga e ervas',
  'https://images.unsplash.com/photo-1637194502327-c99c94943680?w=600&q=80', 19.90, 10, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Bebidas' LIMIT 1), 'Cerveja Artesanal',
  'Cerveja artesanal gelada, lata 350ml',
  'https://images.unsplash.com/photo-1766589220911-74584516c516?w=600&q=80', 16.90, 2, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Sobremesas' LIMIT 1), 'Pudim de Leite',
  'Pudim cremoso de leite condensado com calda de caramelo',
  'https://images.unsplash.com/photo-1763208384514-9b8531d62ee6?w=600&q=80', 22.90, 5, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Combos' LIMIT 1), 'Combo Boi Festão',
  'Picanha + Fritas + Refrigerante + Sobremesa: a escolha do campeão!',
  'https://images.unsplash.com/photo-1764408182227-c1232be04e22?w=600&q=80', 119.90, 30, 'ATIVO'),
 ((SELECT id FROM categoria WHERE nome = 'Combos' LIMIT 1), 'Combo Família',
  '2 porções de costela + fritas + 2 refrigerantes, ideal para dividir',
  'https://images.unsplash.com/photo-1596995804697-27d11d43652e?w=600&q=80', 189.90, 35, 'ATIVO');

-- ---------- Fichas tecnicas ----------
INSERT INTO ficha_tecnica (prato_id, rendimento, modo_preparo)
SELECT id, 1, 'Grelhar na brasa até o ponto desejado, temperar e servir.'
FROM prato
WHERE nome IN ('Picanha na Brasa','Costela Bovina','Filé Mignon Grelhado','Linguiça Toscana','Coração de Frango',
               'Fritas Crocantes','Purê de Mandioca','Cerveja Artesanal','Pudim de Leite','Combo Boi Festão','Combo Família')
  AND id NOT IN (SELECT prato_id FROM ficha_tecnica);

-- Cada linha: prato, ingrediente (sku), quantidade, unidade, fator de correcao.
INSERT INTO ficha_tecnica_item (ficha_tecnica_id, ingrediente_id, quantidade, unidade, fator_correcao)
SELECT ft.id, i.id, v.qtd, v.un, v.fc
FROM (
  SELECT 'Picanha na Brasa' AS prato, 'ING-016' AS sku, 350 AS qtd, 'G' AS un, 1.10 AS fc UNION ALL
  SELECT 'Picanha na Brasa', 'ING-021', 15, 'G', 1.00 UNION ALL
  SELECT 'Picanha na Brasa', 'ING-029', 5, 'G', 1.00 UNION ALL
  SELECT 'Costela Bovina', 'ING-017', 500, 'G', 1.15 UNION ALL
  SELECT 'Costela Bovina', 'ING-029', 10, 'G', 1.00 UNION ALL
  SELECT 'Filé Mignon Grelhado', 'ING-018', 300, 'G', 1.05 UNION ALL
  SELECT 'Filé Mignon Grelhado', 'ING-024', 3, 'G', 1.00 UNION ALL
  SELECT 'Filé Mignon Grelhado', 'ING-023', 15, 'G', 1.00 UNION ALL
  SELECT 'Linguiça Toscana', 'ING-019', 250, 'G', 1.00 UNION ALL
  SELECT 'Linguiça Toscana', 'ING-029', 3, 'G', 1.00 UNION ALL
  SELECT 'Coração de Frango', 'ING-020', 250, 'G', 1.05 UNION ALL
  SELECT 'Coração de Frango', 'ING-029', 5, 'G', 1.00 UNION ALL
  SELECT 'Fritas Crocantes', 'ING-012', 300, 'G', 1.15 UNION ALL
  SELECT 'Fritas Crocantes', 'ING-013', 40, 'ML', 1.00 UNION ALL
  SELECT 'Fritas Crocantes', 'ING-029', 5, 'G', 1.00 UNION ALL
  SELECT 'Purê de Mandioca', 'ING-022', 300, 'G', 1.20 UNION ALL
  SELECT 'Purê de Mandioca', 'ING-023', 20, 'G', 1.00 UNION ALL
  SELECT 'Purê de Mandioca', 'ING-024', 2, 'G', 1.00 UNION ALL
  SELECT 'Cerveja Artesanal', 'ING-025', 1, 'UN', 1.00 UNION ALL
  SELECT 'Pudim de Leite', 'ING-010', 120, 'G', 1.00 UNION ALL
  SELECT 'Pudim de Leite', 'ING-026', 150, 'ML', 1.00 UNION ALL
  SELECT 'Pudim de Leite', 'ING-027', 2, 'UN', 1.00 UNION ALL
  SELECT 'Pudim de Leite', 'ING-028', 40, 'G', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-016', 300, 'G', 1.10 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-012', 250, 'G', 1.15 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-013', 40, 'ML', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-015', 1, 'UN', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-010', 100, 'G', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-026', 100, 'ML', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-027', 1, 'UN', 1.00 UNION ALL
  SELECT 'Combo Boi Festão', 'ING-028', 30, 'G', 1.00 UNION ALL
  SELECT 'Combo Família', 'ING-017', 900, 'G', 1.15 UNION ALL
  SELECT 'Combo Família', 'ING-012', 500, 'G', 1.15 UNION ALL
  SELECT 'Combo Família', 'ING-013', 70, 'ML', 1.00 UNION ALL
  SELECT 'Combo Família', 'ING-015', 2, 'UN', 1.00
) v
JOIN prato p ON p.nome = v.prato
JOIN ficha_tecnica ft ON ft.prato_id = p.id
JOIN ingrediente i ON i.sku = v.sku;

-- ---------- Estoque inicial dos ingredientes novos ----------
INSERT INTO estoque_movimentacao (ingrediente_id, tipo, quantidade, motivo, lote, custo_unitario, usuario_id)
SELECT id, 'ENTRADA',
       CASE WHEN unidade_padrao = 'UN' THEN 150
            WHEN unidade_padrao = 'ML' THEN 8000
            WHEN sku IN ('ING-021','ING-024','ING-029') THEN 1500
            WHEN sku = 'ING-023' THEN 3000
            ELSE 12000 END,
       'AJUSTE', 'LOTE-INICIAL', custo_unitario, 1
FROM ingrediente
WHERE sku IN ('ING-016','ING-017','ING-018','ING-019','ING-020','ING-021','ING-022','ING-023','ING-024',
              'ING-025','ING-026','ING-027','ING-028','ING-029');
