-- Combos com escolhas (como no McDonald's): grupos de escolha por combo e as opcoes de cada grupo.
CREATE TABLE combo_grupo (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    prato_id  BIGINT      NOT NULL,
    nome      VARCHAR(80) NOT NULL,
    minimo    INT         NOT NULL DEFAULT 1,   -- 0 = grupo opcional
    maximo    INT         NOT NULL DEFAULT 1,
    ordem     INT         NOT NULL DEFAULT 0,
    CONSTRAINT fk_cg_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

CREATE TABLE combo_opcao (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    grupo_id   BIGINT        NOT NULL,
    nome       VARCHAR(80)   NOT NULL,
    prato_id   BIGINT        NULL,              -- prato que da baixa no estoque quando escolhido (pode ser nulo)
    acrescimo  DECIMAL(10,2) NOT NULL DEFAULT 0,
    ordem      INT           NOT NULL DEFAULT 0,
    CONSTRAINT fk_co_grupo FOREIGN KEY (grupo_id) REFERENCES combo_grupo (id),
    CONSTRAINT fk_co_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

-- O que o cliente escolheu em cada item do pedido (copia dos nomes, nao muda se o combo for editado depois).
CREATE TABLE pedido_item_opcao (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_item_id  BIGINT        NOT NULL,
    grupo_nome      VARCHAR(80)   NOT NULL,
    opcao_nome      VARCHAR(80)   NOT NULL,
    prato_id        BIGINT        NULL,
    acrescimo       DECIMAL(10,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_pio_item FOREIGN KEY (pedido_item_id) REFERENCES pedido_item (id),
    CONSTRAINT fk_pio_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

-- Refrigerante e sobremesa agora sao escolhidos pelo cliente e dao baixa pela opcao: saem da ficha fixa do combo.
DELETE fti FROM ficha_tecnica_item fti
JOIN ficha_tecnica ft ON ft.id = fti.ficha_tecnica_id
JOIN prato p ON p.id = ft.prato_id
JOIN ingrediente i ON i.id = fti.ingrediente_id
WHERE (p.nome = 'Combo Boi Festão' AND i.sku IN ('ING-015','ING-010','ING-026','ING-027','ING-028'))
   OR (p.nome = 'Combo Família' AND i.sku = 'ING-015');

INSERT INTO combo_grupo (prato_id, nome, minimo, maximo, ordem)
SELECT p.id, g.nome, g.mn, g.mx, g.ord
FROM (
  SELECT 'Combo Boi Festão' AS prato, 'Tamanho da batata' AS nome, 1 AS mn, 1 AS mx, 1 AS ord UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 1, 1, 2 UNION ALL
  SELECT 'Combo Boi Festão', 'Sobremesa', 1, 1, 3 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 2, 2, 1
) g
JOIN prato p ON p.nome = g.prato;

INSERT INTO combo_opcao (grupo_id, nome, prato_id, acrescimo, ordem)
SELECT cg.id, o.opcao, pr.id, o.acr, o.ord
FROM (
  SELECT 'Combo Boi Festão' AS combo, 'Tamanho da batata' AS grupo, 'Pequena' AS opcao, '' AS prato_ref, 0.00 AS acr, 1 AS ord UNION ALL
  SELECT 'Combo Boi Festão', 'Tamanho da batata', 'Média',  '', 3.00, 2 UNION ALL
  SELECT 'Combo Boi Festão', 'Tamanho da batata', 'Grande', '', 6.00, 3 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Coca-Cola', 'Refrigerante Lata 350ml', 0.00, 1 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Guaraná',   'Refrigerante Lata 350ml', 0.00, 2 UNION ALL
  SELECT 'Combo Boi Festão', 'Bebida', 'Sprite',    'Refrigerante Lata 350ml', 0.00, 3 UNION ALL
  SELECT 'Combo Boi Festão', 'Sobremesa', 'Pudim de Leite', 'Pudim de Leite', 0.00, 1 UNION ALL
  SELECT 'Combo Boi Festão', 'Sobremesa', 'Açaí 500ml',     'Açaí 500ml',     0.00, 2 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Coca-Cola', 'Refrigerante Lata 350ml', 0.00, 1 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Guaraná',   'Refrigerante Lata 350ml', 0.00, 2 UNION ALL
  SELECT 'Combo Família', 'Bebidas', 'Sprite',    'Refrigerante Lata 350ml', 0.00, 3
) o
JOIN prato p ON p.nome = o.combo
JOIN combo_grupo cg ON cg.prato_id = p.id AND cg.nome = o.grupo
LEFT JOIN prato pr ON pr.nome = o.prato_ref;
