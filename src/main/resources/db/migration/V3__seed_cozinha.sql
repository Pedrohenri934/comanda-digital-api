-- Usuarios de teste para a cozinha e a gerencia (mesma senha do admin: senha123).
-- Copia o hash do admin para nao precisar gerar BCrypt na mao.
INSERT INTO usuario (nome, email, senha_hash, perfil, status)
SELECT 'Cozinha Teste', 'cozinha@email.com', senha_hash, 'COZINHEIRO', 'ATIVO'
FROM usuario WHERE email = 'admin@email.com';

INSERT INTO usuario (nome, email, senha_hash, perfil, status)
SELECT 'Gerente Teste', 'gerente@email.com', senha_hash, 'GERENTE', 'ATIVO'
FROM usuario WHERE email = 'admin@email.com';
