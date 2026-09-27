# Decisão da Fase 5

## Extensão escolhida

Foi escolhida a integração de notificações de falha via SQS FIFO em LocalStack. Ela aproveita o DLT que já existe no `claim-worker`, cobre uma fronteira AWS útil sem criar um novo domínio de negócio e permanece reproduzível localmente.

O `claim-worker` é dono do processamento do evento e do registro `claim_event_dlt`. Ele publica um contrato sanitizado `claim-notification-v1` em Kafka. O `notification-service` é dono da adaptação desse contrato para SQS. O LocalStack é apenas um emulador local; não há conta, credencial ou endpoint AWS real.

A fila FIFO usa `eventId` como deduplicação e `claimId` como grupo. Isso reduz duplicatas dentro da janela de deduplicação do SQS, mas não transforma a entrega em exatamente uma vez. A publicação Kafka e o envio SQS continuam sendo fronteiras distintas e devem ser tratadas como at-least-once.

## Operação local

O perfil `aws-lab` sobe `localstack` e `notification-service`. A fila é criada de forma idempotente na inicialização do consumidor. Todos os valores AWS são sintéticos e o perfil não é iniciado pelo Compose padrão.

## Mapeamento de produção (não implantado)

Em uma evolução, `notification-service` poderia usar uma fila SQS real com uma IAM task role de menor privilégio, secrets gerenciados fora da imagem e execução em ECS/Fargate. O tópico Kafka, DLT, alarmes de falha e política de retenção seriam definidos separadamente. Este repositório não contém `terraform apply`, não publica imagens e não configura AWS real.
