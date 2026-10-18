# Mercado Payment Service — CP5 Fases 2B/2C

Microservico Java 17/Spring Boot que consome solicitacoes de pagamento do RabbitMQ, chama o simulador REST do Banco Tranquilo e publica o resultado. Este projeto e independente do `mercado-api-II` e nao acessa banco de dados, Render ou configuracoes de producao.

## Contrato preservado

O `payload` de entrada preserva exatamente os campos do `PaymentRequest` da Fase 2A:

```json
{
  "paymentAttemptId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "payload": {
    "purchaseId": 11,
    "correlationId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
    "amount": 39.90,
    "currency": "BRL"
  }
}
```

O resultado preserva os mesmos campos e acrescenta o `status` original `APPROVED` ou `REJECTED` dentro do payload. O `paymentAttemptId` e um UUID criado pelo produtor uma unica vez e mantido em redeliveries e retries. O servico usa essa chave para devolver o resultado ja calculado sem autorizar novamente no banco. Reutilizar a chave com outro payload e tratado como conflito e, depois dos retries, vai para a DLQ.

O cache de idempotencia desta entrega e local ao processo. Em um ambiente distribuido ou com reinicios, substitua-o por armazenamento compartilhado persistente antes de escalar replicas.

## Topologia RabbitMQ

Todos os exchanges e filas abaixo sao duraveis; todas as mensagens publicadas pelo servico sao persistentes.

| Tipo | Nome | Routing key / comportamento |
| --- | --- | --- |
| Direct exchange | `mercado.payments` | `payment.request`, `payment.result` |
| Fila principal | `mercado.payment.requests` | consome `payment.request`; rejeicoes do container seguem para a DLX |
| Fila de resultados | `mercado.payment.results` | recebe `payment.result` |
| Direct exchange | `mercado.payments.retry` | `payment.request.retry` |
| Fila de retry | `mercado.payment.requests.retry` | TTL padrao de 5 s; depois retorna automaticamente para `payment.request` |
| Direct exchange | `mercado.payments.dlx` | `payment.request.dead` |
| Dead-letter queue | `mercado.payment.requests.dlq` | recebe falhas apos 3 retries |

O header `x-payment-retry-count` acompanha a mensagem. O header `x-payment-last-error` registra a ultima falha. Uma mensagem tem uma tentativa inicial mais o numero de retries configurado por `PAYMENT_MAX_RETRIES`.

## Como iniciar

Requisitos: Java 17 e Docker Desktop/Compose.

Clone os dois repositorios como diretorios irmaos para manter os comandos e a referencia da colecao Postman portaveis:

```powershell
git clone https://github.com/GusCrevelari/mercado-payment-service.git
git clone https://github.com/GusCrevelari/banco-tranquilo-simulator.git
```

Os nomes dos diretorios devem permanecer `mercado-payment-service` e `banco-tranquilo-simulator`. A infraestrutura Docker Compose pertence a este repositorio; o simulador e iniciado separadamente a partir do diretorio irmao.

1. Inicie RabbitMQ com Management, PostgreSQL local e Mailpit:

```powershell
cd mercado-payment-service
docker compose up -d
docker compose ps
```

Se a instalacao expuser o executavel standalone, use os comandos equivalentes `docker-compose up -d` e `docker-compose ps`.

Management: `http://localhost:15672` (`guest` / `guest`, apenas para desenvolvimento local).
Mailpit: `http://localhost:8025`; as mensagens sao capturadas localmente e nao sao entregues externamente.
PostgreSQL local: `localhost:5433`; usado somente pelo roteiro da Fase 2C.

2. Em outro terminal, inicie o simulador primeiro:

```powershell
cd ..\banco-tranquilo-simulator
.\mvnw.cmd spring-boot:run
```

3. Em um terceiro terminal, inicie este consumidor:

```powershell
cd ..\mercado-payment-service
.\mvnw.cmd spring-boot:run
```

Health check: `http://localhost:8090/actuator/health`.

As declaracoes de exchanges, filas e bindings sao feitas na inicializacao. Importe `postman/phase-2b.postman_collection.json`, publique um exemplo pela API Management e leia o resultado na fila. Os JSONs equivalentes estao em `examples/`.

## Configuracao

| Variavel | Padrao | Funcao |
| --- | --- | --- |
| `RABBITMQ_HOST` | `localhost` | host do broker |
| `RABBITMQ_PORT` | `5672` | porta AMQP |
| `RABBITMQ_USERNAME` | `guest` | usuario local |
| `RABBITMQ_PASSWORD` | `guest` | senha local |
| `BANCO_TRANQUILO_URL` | `http://localhost:8091` | base URL do simulador |
| `BANCO_TRANQUILO_TIMEOUT` | `3s` | timeout HTTP |
| `PAYMENT_MAX_RETRIES` | `3` | retries depois da tentativa inicial |
| `PAYMENT_RETRY_DELAY` | `5s` | TTL da fila de retry |
| `PORT` | `8090` | porta do actuator |

## Testes e build

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Os testes cobrem serializacao do envelope, idempotencia e conflito da tentativa, publicacao do resultado, retry e envio a DLQ. Nao dependem de RabbitMQ ou do simulador em execucao.

Para parar e manter os dados do broker:

```powershell
docker compose down
```

Com o executavel standalone, use `docker-compose down`.

Para apagar tambem o volume local, execute explicitamente `docker compose down -v`.
