#!/bin/bash

echo "=========================================="
echo "Inicializando servicios AWS en LocalStack"
echo "=========================================="

# Esperar a que LocalStack esté completamente listo
sleep 5

# Configurar AWS CLI para LocalStack
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

echo ""
echo "1. Creando bucket S3: fleet-documents"
awslocal s3 mb s3://fleet-documents
awslocal s3api put-bucket-versioning \
  --bucket fleet-documents \
  --versioning-configuration Status=Enabled

echo ""
echo "2. Creando cola SQS: fleet-solicitudes-queue"
awslocal sqs create-queue \
  --queue-name fleet-solicitudes-queue \
  --attributes VisibilityTimeout=300,MessageRetentionPeriod=1209600

echo ""
echo "3. Obteniendo URL de la cola SQS"
QUEUE_URL=$(awslocal sqs get-queue-url --queue-name fleet-solicitudes-queue --query 'QueueUrl' --output text)
echo "Queue URL: $QUEUE_URL"

echo ""
echo "4. Creando secret en Secrets Manager: fleet-db-credentials"
awslocal secretsmanager create-secret \
  --name fleet-db-credentials \
  --secret-string '{"username":"postgres","password":"Admin123"}'

echo ""
echo "=========================================="
echo "✅ Servicios AWS inicializados correctamente"
echo "=========================================="
echo ""
echo "Recursos creados:"
echo "  - S3 Bucket: fleet-documents"
echo "  - SQS Queue: fleet-solicitudes-queue"
echo "  - Secret: fleet-db-credentials"
echo ""
echo "Endpoints LocalStack:"
echo "  - S3: http://localhost:4566"
echo "  - SQS: http://localhost:4566"
echo "  - Secrets Manager: http://localhost:4566"
echo ""
