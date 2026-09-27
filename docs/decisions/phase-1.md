# Fase 1 - contratos e propriedade dos dados

## Contratos

Os arquivos `contracts/http/consent.yaml`, `policy.yaml` e `claim.yaml` são a fonte API-first. Eles definem os endpoints públicos e a decisão interna síncrona usada por `policy-service` e `claim-service`.

Nesta fase a identidade é simulada por headers. Isso permite testar titularidade, escopo, finalidade, expiração e revogação sem fingir que existe OAuth2. A substituição por JWT/Keycloak está reservada para a Fase 3.

## Propriedade

| Serviço         | Dono                           | Schema         | Regra                                                               |
| --------------- | ------------------------------ | -------------- | ------------------------------------------------------------------- |
| consent-service | consentimentos e decisões      | `consent_data` | só ele cria, consulta, expira e revoga consentimentos               |
| policy-service  | clientes e apólices sintéticos | `policy_data`  | só ele lê apólices; autorização consulta o consent-service por HTTP |
| claim-service   | sinistros                      | `claim_data`   | só ele cria/lê sinistros; não há FK cruzada                         |

Um único PostgreSQL local hospeda os schemas por conveniência. A separação lógica evita joins entre domínios e deixa a migração para bancos distintos explícita. A decisão de autorização é síncrona para que uma revogação bloqueie imediatamente; não há dependência de evento assíncrono nesta fase.

## Limites

Kafka, outbox, retry/DLT, Keycloak, JWT, observabilidade e auditoria não fazem parte desta entrega. `POST /claims` já persiste uma abertura idempotente, mas ainda não publica evento.
