-- Pedidos que a cozinha marcou como "saiu para entrega" sem entregador ficavam travados.
-- Voltam para PRONTO para um entregador poder aceitar.
UPDATE pedido SET status = 'PRONTO' WHERE status = 'SAIU_ENTREGA' AND entregador_id IS NULL;
