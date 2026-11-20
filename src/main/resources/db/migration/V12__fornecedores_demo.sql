-- Dois fornecedores de demonstracao (CNPJs validos) com catalogo, para a cotacao comparativa funcionar de cara.
INSERT INTO fornecedor (razao_social, cnpj, telefone, email, status)
SELECT 'Frigorífico Boi Forte Ltda', '11.222.333/0001-81', '(11) 3000-1000', 'vendas@boiforte.com.br', 'ATIVO'
WHERE NOT EXISTS (SELECT 1 FROM fornecedor WHERE cnpj = '11.222.333/0001-81');

INSERT INTO fornecedor (razao_social, cnpj, telefone, email, status)
SELECT 'Atacado Mesa Farta S.A.', '11.444.777/0001-61', '(11) 3000-2000', 'comercial@mesafarta.com.br', 'ATIVO'
WHERE NOT EXISTS (SELECT 1 FROM fornecedor WHERE cnpj = '11.444.777/0001-61');

-- Boi Forte vende todos os ingredientes ativos ~8% abaixo do custo atual; Mesa Farta ~5% acima.
INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda)
SELECT f.id, g.id, ROUND(GREATEST(g.custo_unitario, 0.0001) * 0.92, 4), g.unidade_padrao
FROM fornecedor f CROSS JOIN ingrediente g
WHERE f.cnpj = '11.222.333/0001-81' AND g.status = 'ATIVO'
  AND NOT EXISTS (SELECT 1 FROM fornecedor_produto p WHERE p.fornecedor_id = f.id AND p.ingrediente_id = g.id);

INSERT INTO fornecedor_produto (fornecedor_id, ingrediente_id, preco, unidade_venda)
SELECT f.id, g.id, ROUND(GREATEST(g.custo_unitario, 0.0001) * 1.05, 4), g.unidade_padrao
FROM fornecedor f CROSS JOIN ingrediente g
WHERE f.cnpj = '11.444.777/0001-61' AND g.status = 'ATIVO'
  AND NOT EXISTS (SELECT 1 FROM fornecedor_produto p WHERE p.fornecedor_id = f.id AND p.ingrediente_id = g.id);
