#!/bin/sh
# Prepara en LocalStack lo que en AWS se crearia aparte: la cola de solicitudes aprobadas (con su DLQ) y la tabla de reportes.
# Es idempotente: se puede repetir. Las credenciales son ficticias y solo valen contra LocalStack.
set -eu

AWS="aws --endpoint-url ${LOCALSTACK_URL:-http://localstack:4566} --region ${AWS_REGION:-us-east-1}"
CUENTA=000000000000
REGION="${AWS_REGION:-us-east-1}"

crear_cola_con_dlq() {
  nombre="$1"
  $AWS sqs create-queue --queue-name "$nombre-dlq" >/dev/null
  dlq_arn="arn:aws:sqs:$REGION:$CUENTA:$nombre-dlq"
  # Tras 3 intentos el mensaje va a la DLQ
  $AWS sqs create-queue --queue-name "$nombre" --attributes "{
    \"VisibilityTimeout\":\"60\",
    \"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$dlq_arn\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }" >/dev/null
  echo "cola $nombre lista"
}

crear_cola_con_dlq crediya-solicitud-aprobada

# Misma forma que la tabla real: clave pk (texto), bajo demanda y TTL en expira_en
if ! $AWS dynamodb describe-table --table-name crediya-reportes >/dev/null 2>&1; then
  $AWS dynamodb create-table --table-name crediya-reportes \
    --attribute-definitions AttributeName=pk,AttributeType=S \
    --key-schema AttributeName=pk,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST >/dev/null
  $AWS dynamodb update-time-to-live --table-name crediya-reportes \
    --time-to-live-specification "Enabled=true,AttributeName=expira_en" >/dev/null
fi
echo "tabla crediya-reportes lista"
