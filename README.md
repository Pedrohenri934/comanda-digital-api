# Comanda Digital — API (Back-end)

Back-end do sistema **Comanda Digital** para Dark Kitchen — Trabalho Final de Desenvolvimento Full-Stack (UNASP-SP, Prof. Thiago Silva).

## Links de produção

| O quê | Endereço |
|---|---|
| Aplicação (front) | https://comanda-digital-front-phi.vercel.app |
| API | https://comanda-digital-api-production-3501f934.up.railway.app |
| Swagger (documentação) | https://comanda-digital-api-production-3501f934.up.railway.app/swagger-ui.html |
| Repositório do front | https://github.com/Pedrohenri934/comanda-digital-front |

## Usuários de teste

| Perfil | E-mail | Senha |
|---|---|---|
| Administrador | admin@email.com | senha123 |
| Gerente | gerente@email.com | senha123 |
| Cozinha | cozinha@email.com | senha123 |
| Cliente | criar conta pela tela de cadastro | — |
| Entregador | carlos@entrega.com | 123456 |
| Entregador | ana@entrega.com | 123456 |
| Entregador (pendente) | joao@entrega.com | 123456 |

## Tecnologias

- Java 17, Spring Boot 3.3.5
- Spring Security + JWT (perfis ADMIN, GERENTE, COZINHEIRO, CLIENTE)
- Spring Data JPA + JDBC (consultas de estoque, compras e dashboard)
- MySQL 8 com migrations **Flyway** (`src/main/resources/db/migration`)
- Swagger / OpenAPI (springdoc)
- Docker (build multi-stage) e deploy na **Railway** (API + MySQL)

## Principais regras de negócio

- **RN03:** pedido só é aceito com estoque suficiente (erro 422 lista os ingredientes em falta).
- Baixa de estoque ao **confirmar** o pedido; **estorno** ao cancelar um pedido já confirmado.
- Cancelamento exige motivo e não é permitido ao perfil Cozinha.
- **RN06:** exclusão lógica (status INATIVO).
- Prato só fica ativo com ficha técnica e categoria ativa.
- Compras: RASCUNHO → ENVIADO → RECEBIDO/CANCELADO; ao receber, entram movimentações de estoque.
- Dashboard: faturamento, ticket médio, pratos mais vendidos, estoque abaixo do mínimo.

## Como rodar localmente

Pré-requisitos: JDK 17 e MySQL 8 rodando em `localhost:3306`.

```powershell
$env:SPRING_DATASOURCE_PASSWORD='sua_senha_do_mysql'
$env:JWT_SECRET='uma-chave-secreta-com-pelo-menos-32-caracteres'
.\mvnw.cmd spring-boot:run
```

O banco `comanda_digital` é criado automaticamente e o Flyway cria as tabelas e os dados iniciais.
Swagger local: http://localhost:8080/swagger-ui.html

## Variáveis de ambiente

| Variável | Para quê | Padrão |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC do MySQL | MySQL local |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | — |
| `JWT_SECRET` | Segredo do JWT (mín. 32 caracteres) | — |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas, separadas por vírgula | `http://localhost:4200` |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `framework` (atrás de proxy HTTPS) | — |
| `PORT` | Porta do servidor | `8080` |

## Deploy (Railway)

Serviço da API construído pelo `Dockerfile` na raiz, mais um serviço MySQL. As variáveis do banco referenciam o serviço MySQL (`${{MySQL.MYSQLHOST}}` etc.).

## Autor

Pedro Henrique — ADS, UNASP-SP.
