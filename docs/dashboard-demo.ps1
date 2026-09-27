param(
    [string]$BaseUrl = 'http://localhost',
    [int]$WaitSeconds = 8
)

$ErrorActionPreference = 'Stop'

function Get-AccessToken {
    (Invoke-RestMethod -Method Post -Uri "$BaseUrl`:8090/realms/openinsure/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ client_id = 'openinsure-cli'; grant_type = 'password'; username = 'customer-001'; password = 'local-demo-only' }).access_token
}

function Get-PrometheusValue([string]$Expression) {
    $encoded = [Uri]::EscapeDataString($Expression)
    $result = Invoke-RestMethod -Uri "$BaseUrl`:9090/api/v1/query?query=$encoded"
    if ($result.data.result.Count -eq 0) { return '0' }
    return $result.data.result[0].value[1]
}

$token = Get-AccessToken
$auth = @{ Authorization = "Bearer $token" }
$runId = [guid]::NewGuid().ToString()
$consentBody = @{ resourceOwnerId = 'customer-001'; purpose = 'claim-assessment'; scopes = @('policy:read', 'claim:write'); expiresAt = '2099-01-01T00:00:00Z' } | ConvertTo-Json
$consent = Invoke-RestMethod -Method Post -Uri "$BaseUrl`:8081/consents" -Headers $auth -ContentType 'application/json' -Body $consentBody
$claimHeaders = @{ Authorization = "Bearer $token"; 'X-Consent-Id' = $consent.id; 'X-Scope' = 'claim:write'; 'X-Purpose' = 'claim-assessment' }

1..3 | ForEach-Object {
    $claimHeaders['Idempotency-Key'] = "dashboard-demo-$runId-claim-$_"
    $body = @{ policyId = '11111111-1111-1111-1111-111111111111'; description = "Dano sintetico demonstrativo $_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$BaseUrl`:8083/claims" -Headers $claimHeaders -ContentType 'application/json' -Body $body | Out-Null
}

# A segunda chamada com a mesma chave demonstra a proteção contra duplicação.
$claimHeaders['Idempotency-Key'] = "dashboard-demo-$runId-claim-1"
$replayBody = @{ policyId = '11111111-1111-1111-1111-111111111111'; description = 'Dano sintetico demonstrativo 1' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$BaseUrl`:8083/claims" -Headers $claimHeaders -ContentType 'application/json' -Body $replayBody | Out-Null

# Evento sintético inválido: retry limitado, DLT e notificação LocalStack.
$eventId = [guid]::NewGuid().ToString()
$invalidEvent = "{`"eventId`":`"$eventId`",`"schemaVersion`":1,`"correlationId`":`"dashboard-demo-$eventId`"}"
$invalidEvent | docker compose exec -T kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:9092 --topic claim-events | Out-Null

Write-Host "Aguardando processamento Kafka/DLT por $WaitSeconds segundos..."
Start-Sleep -Seconds $WaitSeconds

$metrics = [ordered]@{
    claimsCreated = Get-PrometheusValue 'sum(openinsure_claims_total)'
    claimReplays = Get-PrometheusValue 'sum(openinsure_claims_idempotent_replays_total)'
    eventsProcessed = Get-PrometheusValue 'sum(openinsure_worker_events_processed_total)'
    eventsInDlt = Get-PrometheusValue 'sum(openinsure_worker_events_dlt_total)'
    notificationsToSqs = Get-PrometheusValue 'sum(openinsure_notifications_sent_total)'
    outboxPublished = Get-PrometheusValue 'sum(openinsure_outbox_published_total)'
}

Write-Host ''
Write-Host 'Metricas demonstrativas atuais:'
$metrics | Format-Table -AutoSize
Write-Host "Dashboard: $BaseUrl`:3000/d/openinsure-overview/openinsure-overview?orgId=1&from=now-15m&to=now&timezone=browser"
