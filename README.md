# Comanda Digital — API (Spring Boot + MySQL)

Back-end do Trabalho Final de Desenvolvimento Full-Stack (UNASP-SP, Prof. Thiago Silva) — sistema **Boi Na Brasa** (dark kitchen).

- **API no ar:** https://comanda-digital-api-production-3501f934.up.railway.app
- **Swagger (documentação):** https://comanda-digital-api-production-3501f934.up.railway.app/swagger-ui.html
- **Site (front-end):** https://boi-na-brasa-phi.vercel.app
- **Repositório do front-end:** comanda-digital-front

## Tecnologias

Java 17 · Spring Boot 3.3 · Spring Security + JWT · JPA/JDBC · MySQL 8 · Flyway (migrations V1–V12) · springdoc (Swagger) · Railway.

## Como rodar (local)

Requisitos: JDK 17+ e MySQL 8 na porta 3306. No PowerShell, na pasta com o `pom.xml`:

```powershell
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="a_senha_do_seu_mysql"
$env:JWT_SECRET="uma-frase-longa-com-pelo-menos-32-caracteres-aqui-123"
.\mvnw.cmd spring-boot:run
```

O banco é criado sozinho e o Flyway aplica as migrations.

## Variáveis de ambiente

| Variável | Para que serve | Padrão |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL | MySQL local |
| `SPRING_DATASOURCE_USERNAME` | usuário do banco | `root` |
| `SPRING_DATASOURCE_PASSWORD` | senha do banco | (obrigatória) |
| `JWT_SECRET` | chave do JWT (mín. 32 caracteres) | (obrigatória) |
| `CORS_ALLOWED_ORIGINS` | origens do front, separadas por vírgula | `http://localhost:4200` |
| `PORT` | porta do servidor | `8080` |

Em produção, `CORS_ALLOWED_ORIGINS` deve conter o endereço do site (ex.: `https://boi-na-brasa-phi.vercel.app`). Nenhum segredo fica no código (RNF12).

## Usuários de teste

| Perfil | E-mail | Senha |
|---|---|---|
| ADMIN | admin@email.com | senha123 |
| GERENTE | gerente@email.com | senha123 |
| COZINHEIRO | cozinha@email.com | senha123 |
| ENTREGADOR (aprovado) | carlos@entrega.com / ana@entrega.com | 123456 |
| ENTREGADOR (pendente) | joao@entrega.com | 123456 |

## Funcionalidades

- **Autenticação e perfis (RBAC):** login JWT, cadastro de cliente e de entregador (aprovação pelo admin). Perfis: ADMIN, GERENTE, COZINHEIRO, ENTREGADOR, CLIENTE.
- **Usuários internos (RF-041, só ADMIN):** criar, editar, inativar e reativar gerente/cozinheiro/admin; não permite inativar a si mesmo nem ficar sem admin ativo.
- **Cardápio:** categorias, pratos, ficha técnica com food cost (alerta RN02 acima de 35%), cardápio da semana e combos com escolhas (tamanho, bebida, sobremesa).
- **Pedidos:** RECEBIDO → CONFIRMADO → EM_PREPARO → PRONTO → SAIU_ENTREGA → FINALIZADO / CANCELADO. Baixa de estoque na confirmação (RN03) e estorno no cancelamento. Cozinheiro só cancela antes do preparo (RN04). Filtros por status, data e canal.
- **Estoque:** saldo, alertas de mínimo, entrada manual (lote, validade, custo), baixa por perda (desperdício, vencimento, quebra, uso interno) e histórico de movimentações.
- **Fornecedores e compras:** cadastro com validação de CNPJ, catálogo de produtos por fornecedor, cotação comparativa (menor preço) e compras RASCUNHO → ENVIADO → RECEBIDO (a entrada no estoque é automática).
- **Entregadores:** pedidos prontos, aceite, entrega e histórico.
- **Dashboard e relatórios:** vendas, ticket, food cost médio, top pratos e matriz BCG.

## Principais endpoints (todos documentados no Swagger)

| Área | Rotas |
|---|---|
| Auth | `POST /api/auth/login`, `/register`, `/register-entregador`, `GET /api/auth/me` |
| Cardápio (público) | `GET /api/cardapio`, `/api/cardapio/{id}/opcoes` |
| Pedidos (cliente) | `/api/pedidos` |
| Pedidos (gestão) | `GET /api/gestao/pedidos` (= `/api/admin/pedidos`), `PATCH /{id}/status`, `PATCH /{id}/cancelar` |
| Cadastros | `/api/admin/categorias`, `/pratos`, `/pratos/{id}/ficha` (= `/custo`), `/ingredientes` |
| Usuários (ADMIN) | `/api/admin/usuarios` |
| Estoque | `/api/admin/estoque/saldo`, `/alertas`, `/movimentacoes`, `POST /entrada`, `POST /saida` |
| Fornecedores | `/api/admin/fornecedores`, `/fornecedores/{id}/produtos` |
| Cotação | `GET /api/admin/cotacao/{ingredienteId}` |
| Compras | `/api/admin/compras`, `PATCH /{id}/status`, `PATCH /{id}/receber` |
| Dashboard | `GET /api/admin/dashboard` (= `/resumo`), `/api/admin/relatorios/bcg` |

## Banco de dados

Diagrama (DER) em `docs/DER.png` (fonte em `docs/DER.mmd`). Migrations em `src/main/resources/db/migration`.

## Estrutura

```
com.comandadigital.api
  config/      SecurityConfig, CorsConfig, OpenApiConfig
  controller/  endpoints REST
  service/     regras de negócio
  repository/  JPA e JDBC
  model/       entidades
  dto/         request e response
  security/    JWT
  exception/   tratamento global de erros
```
