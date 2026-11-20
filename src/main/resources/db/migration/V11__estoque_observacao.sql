-- Observacao livre nas movimentacoes manuais de estoque (entradas e baixas por perda).
ALTER TABLE estoque_movimentacao ADD COLUMN observacao VARCHAR(255) NULL;
