-- Cardapio da semana: quais pratos aparecem em cada dia e qual e o "especial do dia".
-- dia_semana segue o padrao ISO: 1 = segunda ... 7 = domingo.
CREATE TABLE cardapio_semana (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dia_semana  TINYINT NOT NULL,
    prato_id    BIGINT  NOT NULL,
    destaque    BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_cardapio_dia_prato UNIQUE (dia_semana, prato_id),
    CONSTRAINT fk_cardapio_prato FOREIGN KEY (prato_id) REFERENCES prato (id)
);

-- Ponto de partida: todos os pratos ativos em todos os dias.
INSERT INTO cardapio_semana (dia_semana, prato_id, destaque)
SELECT d.n, p.id, FALSE
FROM (SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7) d
CROSS JOIN prato p
WHERE p.status = 'ATIVO';

-- Especial de cada dia.
UPDATE cardapio_semana cs
JOIN prato p ON p.id = cs.prato_id
SET cs.destaque = TRUE
WHERE (cs.dia_semana = 1 AND p.nome = 'Picanha na Brasa')
   OR (cs.dia_semana = 2 AND p.nome = 'Costela Bovina')
   OR (cs.dia_semana = 3 AND p.nome = 'Filé Mignon Grelhado')
   OR (cs.dia_semana = 4 AND p.nome = 'Linguiça Toscana')
   OR (cs.dia_semana = 5 AND p.nome = 'Combo Boi Festão')
   OR (cs.dia_semana = 6 AND p.nome = 'Combo Família')
   OR (cs.dia_semana = 7 AND p.nome = 'Costela Bovina');
