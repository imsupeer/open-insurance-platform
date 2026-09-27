# Decisão da Fase 3

## Identidade e autorização

O laboratório usa Keycloak 26.7.4 como provedor OIDC local. Os três serviços são resource servers Spring Security; validam assinatura, emissor e expiração do JWT. A identidade lógica é `preferred_username` (fallback para `sub`) e, neste ambiente, o usuário sintético é `customer-001`.

Os escopos são `consent:read`, `consent:write`, `policy:read`, `claim:read` e `claim:write`. A ausência de token retorna `401 application/problem+json`; token válido sem o escopo exigido retorna `403`. O dono do domínio continua responsável pela decisão: consent-service decide consentimento, policy-service possui apólices e claim-service possui sinistros.

Chamadas internas de policy/claim para consent-service propagam o mesmo bearer token. A revogação é consultada de modo síncrono antes de retornar dados protegidos; portanto, não depende de evento assíncrono para bloquear acesso.

## Compatibilidade de contratos

Os OpenAPI em `contracts/http` são a fonte de verdade e agora descrevem OAuth2 password flow apenas para obter tokens da fixture local. Não é um fluxo recomendado para produção. O evento `ClaimCreated v1` recebeu `correlationId` opcional; a mudança é compatível com consumidores existentes e não altera campos obrigatórios nem a semântica da versão.

## Limites

Keycloak, PostgreSQL e Kafka são dependências Docker locais. As credenciais do realm são sintéticas e estão no arquivo de demonstração; não representam segredo de produção. Não há AWS, Claude, Itaú, introspecção remota, rotação operacional de chaves ou ambiente produtivo.
