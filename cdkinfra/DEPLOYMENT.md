# 🚀 Guía de Despliegue - Fleet Management Infrastructure

## 📋 Tabla de Contenidos
- [Arquitectura](#arquitectura)
- [Recursos Desplegados](#recursos-desplegados)
- [Prerrequisitos](#prerrequisitos)
- [Configuración Inicial](#configuración-inicial)
- [Despliegue](#despliegue)
- [Validación](#validación)
- [Costos Estimados](#costos-estimados)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                         Internet                             │
└────────────────────────┬────────────────────────────────────┘
                         │
                    ┌────▼────┐
                    │   ALB   │ (Port 80)
                    └────┬────┘
                         │
        ┌────────────────┴────────────────┐
        │                                 │
   ┌────▼────┐                       ┌────▼────┐
   │ ECS Task│                       │ ECS Task│
   │ (Java)  │                       │ (Java)  │
   └────┬────┘                       └────┬────┘
        │                                 │
        └────────────────┬────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐      ┌────▼────┐     ┌────▼────┐
   │ Aurora  │      │   S3    │     │   SQS   │
   │Postgres │      │ Bucket  │     │  Queue  │
   └─────────┘      └─────────┘     └─────────┘
```

### Componentes Principales

1. **VPC Multi-AZ**
   - 2 Availability Zones
   - Subnets públicas y privadas
   - 1 NAT Gateway

2. **Application Load Balancer (ALB)**
   - Internet-facing
   - Health checks configurados
   - Target Group con ECS

3. **ECS Fargate**
   - 2 tareas (alta disponibilidad)
   - Auto-scaling configurado
   - Container Insights habilitado

4. **Aurora PostgreSQL Serverless v2**
   - 1 Writer + 1 Reader
   - Auto-scaling: 0.5 - 2 ACU
   - Backups automáticos (7 días)
   - Credenciales en Secrets Manager

5. **S3 Bucket**
   - Encriptación S3-managed
   - Versionado habilitado
   - Lifecycle: 90 días

6. **SQS Queue**
   - Encriptación KMS
   - Retention: 14 días
   - Visibility timeout: 5 min

---

## 📦 Recursos Desplegados

| Recurso | Tipo | Descripción |
|---------|------|-------------|
| VPC | `AWS::EC2::VPC` | Red virtual con 2 AZs |
| ALB | `AWS::ElasticLoadBalancingV2::LoadBalancer` | Balanceador de carga |
| ECS Cluster | `AWS::ECS::Cluster` | Cluster Fargate |
| ECS Service | `AWS::ECS::Service` | 2 tareas Java |
| Aurora Cluster | `AWS::RDS::DBCluster` | PostgreSQL 15.4 |
| S3 Bucket | `AWS::S3::Bucket` | Almacenamiento documentos |
| SQS Queue | `AWS::SQS::Queue` | Cola de mensajes |
| Secrets Manager | `AWS::SecretsManager::Secret` | Credenciales DB |

---

## ✅ Prerrequisitos

### 1. AWS CLI Configurado
```bash
aws configure
# Ingresar:
# - AWS Access Key ID
# - AWS Secret Access Key
# - Default region (ej: us-east-1)
# - Default output format (json)
```

### 2. Node.js y npm
```bash
node --version  # v18+ requerido
npm --version   # v9+ requerido
```

### 3. AWS CDK CLI
```bash
npm install -g aws-cdk
cdk --version
```

### 4. Bootstrap de CDK (primera vez)
```bash
cdk bootstrap aws://ACCOUNT-ID/REGION
```

---

## ⚙️ Configuración Inicial

### 1. Instalar Dependencias
```bash
cd cdkinfra
npm install
```

### 2. Configurar Región (Opcional)
Editar `bin/infracdk.ts`:
```typescript
new InfracdkStack(app, 'InfracdkStack', {
  env: { 
    account: process.env.CDK_DEFAULT_ACCOUNT, 
    region: 'us-east-1'  // Cambiar región aquí
  },
});
```

### 3. Personalizar Imagen Docker (Importante)
Editar `lib/infracdk-stack.ts` línea 92:
```typescript
image: ecs.ContainerImage.fromRegistry('YOUR_ECR_REPO/app:latest'),
```

**Opciones:**
- ECR: `123456789012.dkr.ecr.us-east-1.amazonaws.com/fleet-app:latest`
- Docker Hub: `username/fleet-app:latest`
- Public ECR: `public.ecr.aws/your-alias/fleet-app:latest`

---

## 🚀 Despliegue

### 1. Validar Sintaxis
```bash
npm run build
```

### 2. Ver Cambios (Dry Run)
```bash
cdk diff
```

### 3. Sintetizar Template CloudFormation
```bash
cdk synth
```

### 4. Desplegar Stack
```bash
cdk deploy
```

**Confirmación:** Se te pedirá aprobar cambios de seguridad (IAM roles, Security Groups).

```
Do you wish to deploy these changes (y/n)? y
```

**Tiempo estimado:** 15-20 minutos

### 5. Obtener Outputs
```bash
aws cloudformation describe-stacks \
  --stack-name InfracdkStack \
  --query 'Stacks[0].Outputs'
```

---

## ✔️ Validación

### 1. Verificar ALB
```bash
# Obtener DNS del ALB
ALB_DNS=$(aws cloudformation describe-stacks \
  --stack-name InfracdkStack \
  --query 'Stacks[0].Outputs[?OutputKey==`LoadBalancerDNS`].OutputValue' \
  --output text)

echo "ALB DNS: $ALB_DNS"

# Probar endpoint
curl http://$ALB_DNS/actuator/health
```

### 2. Verificar Base de Datos
```bash
# Obtener endpoint
DB_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name InfracdkStack \
  --query 'Stacks[0].Outputs[?OutputKey==`DatabaseEndpoint`].OutputValue' \
  --output text)

echo "Database Endpoint: $DB_ENDPOINT"
```

### 3. Verificar Credenciales
```bash
aws secretsmanager get-secret-value \
  --secret-id fleet-db-credentials \
  --query SecretString \
  --output text | jq .
```

### 4. Verificar ECS Tasks
```bash
aws ecs list-tasks \
  --cluster InfracdkStack-FleetCluster \
  --service-name InfracdkStack-Service
```

### 5. Ver Logs
```bash
aws logs tail /aws/ecs/fleet-service --follow
```

---

## 💰 Costos Estimados

### Costo Mensual Aproximado (us-east-1)

| Servicio | Configuración | Costo/mes |
|----------|---------------|-----------|
| **Aurora Serverless v2** | 0.5-2 ACU, 2 instancias | ~$50-150 |
| **ECS Fargate** | 2 tareas (0.5 vCPU, 2GB RAM) | ~$30 |
| **ALB** | 1 ALB + tráfico | ~$20 |
| **NAT Gateway** | 1 NAT + tráfico | ~$35 |
| **S3** | 10GB storage + requests | ~$1 |
| **SQS** | 1M requests | ~$0.40 |
| **Secrets Manager** | 1 secret | ~$0.40 |
| **CloudWatch Logs** | 5GB logs | ~$2.50 |
| **Total** | | **~$140-240/mes** |

**Notas:**
- Costos variables según uso real
- Aurora Serverless escala automáticamente
- Considerar costos de transferencia de datos

---

## 🔧 Troubleshooting

### Error: "writer must be provided"
**Solución:** Ya corregido en el código actual. Aurora Serverless v2 requiere instancias writer/reader explícitas.

### Error: "No default VPC"
```bash
# Crear VPC default
aws ec2 create-default-vpc
```

### Error: "Insufficient capacity"
**Causa:** No hay capacidad Fargate en la región/AZ.
**Solución:** Cambiar región o reintentar más tarde.

### Tasks no inician
```bash
# Ver eventos del servicio
aws ecs describe-services \
  --cluster InfracdkStack-FleetCluster \
  --services InfracdkStack-Service \
  --query 'services[0].events[0:5]'
```

### Health checks fallan
**Verificar:**
1. Imagen Docker tiene endpoint `/actuator/health`
2. Contenedor expone puerto 8080
3. Security Groups permiten tráfico

### Conectividad a Base de Datos
```bash
# Verificar Security Group
aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=*SolicitudesDB*" \
  --query 'SecurityGroups[0].IpPermissions'
```

---

## 🗑️ Destruir Infraestructura

### Eliminar Stack
```bash
cdk destroy
```

**⚠️ ADVERTENCIA:**
- Aurora creará snapshot final (configurable)
- S3 bucket debe estar vacío
- Confirmar eliminación cuando se solicite

### Forzar Eliminación de S3
```bash
BUCKET_NAME=$(aws cloudformation describe-stacks \
  --stack-name InfracdkStack \
  --query 'Stacks[0].Outputs[?OutputKey==`S3BucketName`].OutputValue' \
  --output text)

aws s3 rm s3://$BUCKET_NAME --recursive
```

---

## 📚 Referencias

- [AWS CDK Documentation](https://docs.aws.amazon.com/cdk/)
- [ECS Fargate Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/)
- [Aurora Serverless v2](https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/aurora-serverless-v2.html)
- [Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/)

---

## 🔐 Seguridad

### Mejores Prácticas Implementadas

✅ **Secrets Manager** para credenciales  
✅ **IAM Roles** con mínimo privilegio  
✅ **Security Groups** restrictivos  
✅ **S3** con encriptación y sin acceso público  
✅ **SQS** con encriptación KMS  
✅ **Aurora** en subnets privadas  
✅ **VPC** con subnets públicas/privadas separadas  
✅ **Container Insights** habilitado  

### Recomendaciones Adicionales

- [ ] Habilitar AWS WAF en ALB
- [ ] Configurar AWS Shield para DDoS
- [ ] Implementar AWS Config para compliance
- [ ] Habilitar GuardDuty para detección de amenazas
- [ ] Configurar CloudTrail para auditoría
- [ ] Implementar backup automatizado adicional

---

**Última actualización:** 2026-02-15  
**Versión CDK:** 2.238.0  

