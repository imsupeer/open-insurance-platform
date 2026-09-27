# Demonstração da Fase 3

Todos os valores são sintéticos. A execução abaixo depende de Docker Desktop.

## Subir e obter token

```powershell
Copy-Item .env.example .env
docker compose config
docker compose up --build -d
docker compose ps
Invoke-WebRequest http://localhost:8081/actuator/health/readiness
Invoke-WebRequest http://localhost:8090/health/ready
$fullToken = (Invoke-RestMethod -Method Post -Uri http://localhost:8090/realms/openinsure/protocol/openid-connect/token -ContentType 'application/x-www-form-urlencoded' -Body @{ client_id='openinsure-cli'; grant_type='password'; username='customer-001'; password='local-demo-only' }).access_token
$policyToken = (Invoke-RestMethod -Method Post -Uri http://localhost:8090/realms/openinsure/protocol/openid-connect/token -ContentType 'application/x-www-form-urlencoded' -Body @{ client_id='openinsure-policy-cli'; grant_type='password'; username='customer-001'; password='local-demo-only' }).access_token
```

O realm local importa `customer-001` / `local-demo-only`. O administrador local é `admin` / `openinsure-admin-local-only`; são credenciais de laboratório.

## 401 e 403

```powershell
try { Invoke-WebRequest http://localhost:8082/policies/11111111-1111-1111-1111-111111111111 -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
try { Invoke-WebRequest http://localhost:8083/claims -Method Post -Headers @{ Authorization="Bearer $policyToken" } -ContentType 'application/json' -Body '{}' -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
```

Os resultados esperados são `401` sem bearer token e `403` com token válido sem `claim:write`.

## Conceder, consultar e revogar

```powershell
$auth = @{ Authorization = "Bearer $fullToken" }
$body = '{"resourceOwnerId":"customer-001","purpose":"claim-assessment","scopes":["policy:read","claim:write"],"expiresAt":"2099-01-01T00:00:00Z"}'
$consent = Invoke-RestMethod -Method Post -Uri http://localhost:8081/consents -Headers $auth -ContentType 'application/json' -Body $body
$policyHeaders = @{ Authorization="Bearer $fullToken"; 'X-Consent-Id'=$consent.id; 'X-Scope'='policy:read'; 'X-Purpose'='claim-assessment' }
Invoke-RestMethod -Uri http://localhost:8082/policies/11111111-1111-1111-1111-111111111111 -Headers $policyHeaders | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8081/consents/$($consent.id)/revoke" -Headers $auth | ConvertTo-Json
try { Invoke-WebRequest -Uri http://localhost:8082/policies/11111111-1111-1111-1111-111111111111 -Headers $policyHeaders -UseBasicParsing } catch { $_.Exception.Response.StatusCode.value__ }
```

A consulta inicial retorna a apólice; após a revogação, a decisão síncrona do consent-service retorna `403`.

## Sinistro e Fase 2

Com consentimento ativo, use `POST /claims` com `Authorization`, `Idempotency-Key`, `X-Consent-Id`, `X-Scope=claim:write` e `X-Purpose=claim-assessment`. Outbox, Kafka, retry, DLT e deduplicação continuam disponíveis.

```powershell
$claimHeaders = @{ Authorization="Bearer $fullToken"; 'X-Consent-Id'=$consent.id; 'X-Scope'='claim:write'; 'X-Purpose'='claim-assessment'; 'Idempotency-Key'='phase3-claim-001' }
$claim = Invoke-RestMethod -Method Post -Uri http://localhost:8083/claims -Headers $claimHeaders -ContentType 'application/json' -Body '{"policyId":"11111111-1111-1111-1111-111111111111","description":"Dano sintético"}'
$claim | ConvertTo-Json
```

## Observabilidade da Fase 4

Suba o ambiente com os perfis de observabilidade e LocalStack:

```powershell
docker compose --profile observability --profile aws-lab up --build -d
docker compose ps
Invoke-WebRequest http://localhost:8083/actuator/prometheus
```

As respostas HTTP incluem `X-Correlation-Id` e `traceparent`. Ao abrir um sinistro, o mesmo `correlationId` é gravado no evento de outbox e reaparece no log do `claim-worker`. Os logs são JSON estruturados e não devem conter `Authorization`, tokens, CPF, nomes pessoais ou descrições sensíveis.

Abra o dashboard em [http://localhost:3000/d/openinsure-overview/openinsure-overview](http://localhost:3000/d/openinsure-overview/openinsure-overview). Prometheus fica em [http://localhost:9090](http://localhost:9090).

Na seção `Synthetic business flow`, execute a sequência de consentimento, consulta de apólice e criação de sinistro acima para popular os contadores demonstrativos. Os identificadores e descrições usados na demonstração são sintéticos.

O perfil precisa de Docker e seus volumes são locais. A implementação correlaciona spans por `traceparent`, mas não persiste traces em Jaeger/OpenTelemetry nesta fase.

### Popular o painel com um cenário completo

Para criar três claims, repetir uma chamada com a mesma `Idempotency-Key`, publicar um evento inválido para exercitar retry/DLT e aguardar a notificação no SQS do LocalStack:

```powershell
pwsh -File ./docs/dashboard-demo.ps1
```

Abra o dashboard com a janela `now-15m`. Os dados são fictícios e locais; o evento inválido é intencional para tornar o caminho DLT visível.

## Extensão AWS local da Fase 5

Suba o perfil opcional. O serviço cria uma fila FIFO no LocalStack; não são usadas credenciais AWS reais.

```powershell
docker compose --profile aws-lab up --build -d
docker compose --profile aws-lab ps
docker compose exec localstack awslocal sqs list-queues
```

Para produzir uma mensagem inválida com `eventId` válido e exercitar o DLT, publique um evento sem `claimId`. O worker fará as tentativas configuradas e o `notification-service` encaminhará a notificação sanitizada para SQS:

```powershell
$eventId = [guid]::NewGuid().ToString()
$payload = "{`"eventId`":`"$eventId`",`"schemaVersion`":1}"
$payload | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9092 --topic claim-events
Start-Sleep -Seconds 8
docker compose logs --no-color claim-worker notification-service
$queueUrl = docker compose exec -T localstack awslocal sqs get-queue-url --queue-name openinsure-claim-notifications.fifo | ConvertFrom-Json
docker compose exec -T localstack awslocal sqs receive-message --queue-url $queueUrl.QueueUrl --max-number-of-messages 10
```

O evento é propositalmente sintético e incompleto. A demonstração cobre a extensão LocalStack e o caminho DLT; não é um teste de exatamente-uma-entrega nem uma prova de operação AWS produtiva. O serviço não inclui simulator de seguradora instável porque a Fase 5 mantém uma única extensão pequena e verificável.

O painel de lag usa um indicador sintético local do worker: `0` significa que o fluxo demonstrativo terminou sem pendências. Não é uma coleta JMX do Kafka nem uma garantia de lag de produção.

## Catálogo multi-provider sintético

O `provider-service` apresenta uma visão unificada de três participantes fictícios: Itaú, Bradesco Seguros e Banco do Brasil. Itaú e Bradesco Seguros possuem apólices e sinistros demonstrativos; o BB representa a capacidade de pagamentos de prêmio.

```powershell
Invoke-RestMethod http://localhost:8084/providers | ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8084/providers/itau/policies | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/providers/bradesco-seguros/claims | ConvertTo-Json
Invoke-RestMethod http://localhost:8084/providers/banco-do-brasil | ConvertTo-Json
```

As respostas usam `environment=prod-like-local` e `dataClassification=fictitious`. Nenhuma chamada externa é feita. O dashboard possui a seção `Synthetic provider catalog`, com séries de apólices, sinistros e eventos por provider.

## Encerrar e limites

```powershell
./mvnw.cmd verify
docker compose down
```

`verify`, `docker compose config` e a demonstração dependem de Docker; `./mvnw.cmd -DskipTests package` não. Não há rotação produtiva de chaves, AWS, Claude, Itaú ou dados reais. Password grant é usado somente para a fixture local.
