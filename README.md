

# API Gateway - O Leiloeiro Online

## 📋 Sobre o Projeto

O API Gateway é o ponto central de entrada para todos os microsserviços do ecossistema **O Leiloeiro Online**. Ele atua como um **roteador inteligente**, responsável por:
```
- ✅ **Roteamento de requisições** para os microsserviços apropriados
- ✅ **Autenticação e autorização** via OAuth2 com Keycloak
- ✅ **Gerenciamento de sessão** com cookies HttpOnly
- ✅ **Injeção de headers customizados** (`X-User-Id`, `X-User-Roles`, etc.)
- ✅ **Correlation ID** para rastreamento distribuído
- ✅ **Circuit Breaker** com Resilience4j
- ✅ **CORS** configurado para o frontend
- ✅ **Métricas** via Actuator/Prometheus
```


## 🏗️ Arquitetura
```
┌─────────────────────────────────────────────────────────────────┐
│                    Frontend (HTML/CSS/JS)                      │
│                         localhost:3000                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        API GATEWAY                             │
│                         localhost:9999                         │
├─────────────────────────────────────────────────────────────────┤
│  • Autenticação OAuth2 (Keycloak)                             │
│  • Sessão (Cookie JSESSIONID)                                 │
│  • Injeção de headers X-User-*                                │
│  • Correlation ID (X-Correlation-Id)                          │
│  • Circuit Breaker (Resilience4j)                             │
│  • CORS                                                       │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│ User Service  │    │Auction Service│    │Listing Service│
│   :8080       │    │   :8081       │    │   :8082       │
└───────────────┘    └───────────────┘    └───────────────┘
        │                     │                     │
        └─────────────────────┼─────────────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Keycloak                               │
│                   http://localhost:8081                        │
│                  (Realm: leilao-service)                       │
└─────────────────────────────────────────────────────────────────┘
```

## 🔄Fluxo de autenticação
```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│ Frontend │    │ Gateway  │    │ Keycloak │    │ UserSvc  │
└────┬─────┘    └────┬─────┘    └────┬─────┘    └────┬─────┘
     │               │               │               │
     │  1. GET /login│               │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │  2. Redirect  │               │
     │<──────────────│               │               │
     │               │               │               │
     │  3. Follow redirect           │               │
     │──────────────────────────────>│               │
     │               │               │               │
     │               │               │  4. Login     │
     │               │               │<───┐          │
     │               │               │    │          │
     │               │               │  5. Code      │
     │               │               │───>│          │
     │               │               │               │
     │  6. Redirect com code         │               │
     │<──────────────────────────────│               │
     │               │               │               │
     │  7. GET /callback             │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │  8. Troca code por token      │
     │               │──────────────>│               │
     │               │               │               │
     │               │  9. Access/ID Token           │
     │               │<──────────────│               │
     │               │               │               │
     │               │  10. Busca status             │
     │               │──────────────────────────────>│
     │               │               │               │
     │               │  11. Status usuário           │
     │               │<──────────────────────────────│
     │               │               │               │
     │               │  12. Cria sessão (JSESSIONID) │
     │               │───┐           │               │
     │               │   │           │               │
     │  13. Redirect /home           │               │
     │<──────────────│   │           │               │
     │               │   │           │               │
     │  14. Página inicial           │               │
     │──────────────>│               │               │
     │               │  15. Valida sessão            │
     │               │───┘           │               │
     │               │               │               │
     │  16. Conteúdo protegido       │               │
     │<──────────────│               │               │
```


## 🚀 Tecnologias Utilizadas

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| **Spring Boot** | 4.0.6 | Framework principal |
| **Spring Security** | - | Autenticação e autorização |
| **Spring Cloud Gateway** | - | Roteamento e filtros |
| **Spring Cloud Netflix Eureka** | - | Service Discovery (cliente) |
| **Keycloak** | 26.0.9 | Identity Provider (OAuth2) |
| **Resilience4j** | - | Circuit Breaker |
| **Micrometer** | - | Métricas e monitoramento |
| **Lombok** | - | Redução de boilerplate |
| **Java** | 25 | Linguagem |

## 🔐 Autenticação e Autorização

### Fluxo de Login (OAuth2 Authorization Code Flow)

```
1. Frontend → Gateway: Redireciona para /oauth2/authorization/keycloak
2. Gateway → Keycloak: Redireciona para tela de login
3. Usuário → Keycloak: Insere credenciais
4. Keycloak → Gateway: Redireciona com código de autorização
5. Gateway → Keycloak: Troca código por tokens (internamente)
6. Gateway: Cria sessão (Cookie JSESSIONID)
7. Gateway → Frontend: Redireciona para homepage
```

## 📂 Estrutura do Projeto

```
src/main/java/br/com/infnet/api_gateway/
├── ApiGatewayApplication.java          # Classe principal
├── auth/
│   ├── CustomAuthenticationSuccessHandler.java  # Pós-login
│   └── KeycloakLogoutSuccessHandler.java        # Logout
├── config/
│   ├── CorsConfig.java                # Configuração CORS
│   ├── GatewayRoutesConfig.java       # Rotas do Gateway
│   └── SecurityConfig.java            # Configuração de segurança
├── dto/
│   └── UserStatusResponse.java        # DTO de status do usuário
├── filter/
│   ├── CorrelationIdFilter.java       # Correlation ID
│   └── UserHeadersFilter.java         # Injeção de headers
├── service/
│   └── UserStatusService.java         # Serviço de status do usuário
└── util/
    └── CorrelationIdUtil.java         # Utilidade de Correlation ID
```

---

## 🔒 Headers Customizados

O Gateway injeta os seguintes headers para os microsserviços:

| Header | Origem | Descrição |
|--------|--------|-----------|
| `X-User-Id` | JWT (sub) | ID do usuário autenticado |
| `X-User-Email` | JWT (email) | Email do usuário |
| `X-User-Name` | JWT (name) | Nome do usuário |
| `X-User-Status` | User Service | Status do usuário (ATIVO, SUSPENSO, etc.) |
| `X-User-Allowed` | User Service | Permissão de acesso |
| `X-Correlation-Id` | Gerado | ID de rastreamento distribuído |

---

## 📊 Métricas e Monitoramento

### Endpoints Actuator

| Endpoint | Descrição |
|----------|-----------|
| `/actuator/health` | Health check |
| `/actuator/info` | Informações da aplicação |
| `/actuator/metrics` | Métricas da aplicação |
| `/actuator/prometheus` | Métricas no formato Prometheus |

### Métricas Disponíveis

- Requisições HTTP (tempo, status, URI)
- JVM (memória, GC, threads)
- Circuit Breaker (estado, chamadas, falhas)
- Sistema (CPU, uptime)

---

## 🚀 Como Executar

### Com Docker Compose

```bash
# Subir todos os serviços
docker-compose up -d

# Parar
docker-compose down
```



### Rota Pública

```bash
curl -X POST http://localhost:9999/usuarios/novo \
  -H "Content-Type: application/json" \
  -d '{"username":"teste","email":"teste@teste.com","senha":"123"}'
```

### Rota Protegida (com sessão)

```bash
# Primeiro faça login via navegador
# Depois use o cookie JSESSIONID
curl http://localhost:9999/usuarios/me \
  -H "Cookie: JSESSIONID=seu-cookie-aqui"
```

### Métricas Prometheus

```bash
curl http://localhost:9999/actuator/prometheus
```

---

## 🛡️ Segurança

- **Cookies HttpOnly**: Sessão não acessível via JavaScript
- **CSRF**: Desabilitado para APIs REST
- **CORS**: Configurado para permitir apenas origens confiáveis
- **OAuth2**: Autenticação via Keycloak
- **JWT**: Apenas para serviços internos (não exposto ao frontend)

## 📋 Rotas do Gateway

### Públicas (sem autenticação)

| Método | Rota | Serviço Destino |
|--------|------|-----------------|
| GET | `/health` | Gateway |
| GET | `/actuator/**` | Gateway |
| POST | `/usuarios/novo` | user-service |
| GET | `/usuarios/listar-usernames` | user-service |
| GET | `/usuarios/{id}/perfil` | user-service |
| GET | `/usuarios/{id}/seller-info` | user-service |
| GET | `/auctions/{auctionId}` | auction-service |
| GET | `/listings/**` | listing-service |
| GET | `/recommendations/**` | recommendation-service |

### Privadas (requer autenticação)

| Método | Rota | Serviço Destino |
|--------|------|-----------------|
| GET | `/usuarios/me` | user-service |
| DELETE | `/usuarios/deletar/{id}` | user-service |
| GET | `/usuarios/listar-pfps` | user-service |
| PUT | `/usuarios/trocar-pfp` | user-service |
| POST | `/auctions/create` | auction-service |
| POST | `/auctions/{id}/renew` | auction-service |
| POST | `/auctions/{id}/bids/place` | auction-service |
| DELETE | `/auctions/{id}` | auction-service |
| POST | `/payments/**` | payment-service |
| GET | `/transactions/**` | payment-service |
| POST | `/transactions/**` | payment-service |
| POST | `/report-auction/**` | report-service |
| POST | `/report-message/**` | report-service |

### Logout

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/logout` | Encerra sessão e redireciona para o frontend |

---

## 🐛 Troubleshooting

### Erro: "Invalid token issuer"

Configure `KEYCLOAK_HOSTNAME=keycloak` no Keycloak e use `KEYCLOAK_INTERNAL_URL` para o issuer.

### Erro: 401 na hora de criar o usuário

Abra o painel do keycloak em `localhost:8081`, entre as configurações de admin presentes no .env e regere os `CLIENT_SECRET` para o user-service e para o api-gateway. Para poupar trabalho, copie os valores e os cole dentro do `realm-import.json`.

### Erro: 403 em rotas públicas

Verifique se a rota está no `securityMatcher` do `publicFilterChain`.

### Erro: Sessão não persistente

Certifique-se de que o frontend está enviando `credentials: 'include'` nas requisições.

---

## 📝 Licença

Este projeto é parte do ecossistema **O Leiloeiro Online** e é de uso interno.

---

## 👥 Contribuidores

- Larissa Conti - desenvolvedora principal deste microsserviço

---
