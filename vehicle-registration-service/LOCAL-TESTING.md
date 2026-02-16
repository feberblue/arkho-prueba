# 🧪 Guía de Pruebas Locales - Fleet Management

## 📋 Índice
- [Descripción](#descripción)
- [Arquitectura Local](#arquitectura-local)
- [Prerrequisitos](#prerrequisitos)
- [Inicio Rápido](#inicio-rápido)
- [Pruebas de la API](#pruebas-de-la-api)
- [Verificación de Servicios AWS](#verificación-de-servicios-aws)
- [Troubleshooting](#troubleshooting)

---

## 🎯 Descripción

Esta guía te permite probar **toda la solución sin necesidad de una cuenta AWS**, utilizando:
- **LocalStack**: Simula servicios AWS (S3, SQS, Secrets Manager)
- **PostgreSQL**: Base de datos real en contenedor
- **Docker Compose**: Orquesta todos los servicios

---

## 🏗️ Arquitectura Local

```
┌─────────────────────────────────────────────────────┐
│                   Tu Máquina                         │
│                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────┐ │
│  │   Postgres   │  │  LocalStack  │  │  Java App │ │
│  │   :5432      │  │   :4566      │  │   :8080   │ │
│  │              │  │              │  │           │ │
│  │  - Fleet DB  │  │  - S3        │  │  - REST   │ │
│  │              │  │  - SQS       │  │  - API    │ │
│  │              │  │  - Secrets   │  │           │ │
│  └──────────────┘  └──────────────┘  └───────────┘ │
│         ▲                 ▲                ▲         │
│         └─────────────────┴────────────────┘         │
│              Docker Network: fleet-network           │
└─────────────────────────────────────────────────────┘
```

---

## ✅ Prerrequisitos

### 1. Docker Desktop Instalado
```bash
docker --version
# Docker version 20.10+ requerido
```

### 2. Docker Compose
```bash
docker-compose --version
# Docker Compose version 2.0+ requerido
```

### 3. Verificar Puertos Disponibles
Los siguientes puertos deben estar libres:
- `5432` - PostgreSQL
- `4566` - LocalStack
- `8080` - Aplicación Java

```powershell
# Verificar puertos en Windows
netstat -ano | findstr "5432 4566 8080"
```

---

## 🚀 Inicio Rápido

### Paso 1: Construir y Levantar Servicios

```bash
cd f:\arkho\arkho-prueba\vehicle-registration-service

# Construir y levantar todos los servicios
docker-compose up --build -d
```

**Tiempo estimado:** 3-5 minutos

### Paso 2: Verificar Estado de Contenedores

```bash
docker-compose ps
```

**Salida esperada:**
```
NAME                  STATUS              PORTS
fleet-postgres        Up (healthy)        0.0.0.0:5432->5432/tcp
fleet-localstack      Up (healthy)        0.0.0.0:4566->4566/tcp
fleet-app             Up                  0.0.0.0:8080->8080/tcp
```

### Paso 3: Ver Logs de Inicialización

```bash
# Ver logs de todos los servicios
docker-compose logs -f

# Ver logs solo de la aplicación
docker-compose logs -f app

# Ver logs de LocalStack
docker-compose logs -f localstack
```

---

## 🧪 Pruebas de la API

### 1. Health Check

```bash
curl http://localhost:8080/api/v1/solicitudes
```

**Respuesta esperada:**
```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 0
}
```

### 2. Crear una Solicitud

```bash
curl -X POST http://localhost:8080/api/v1/solicitudes `
  -H "Content-Type: application/json" `
  -d '{
    "nombrePropietario": "Juan Pérez",
    "rut": "12345678-9",
    "email": "juan.perez@example.com",
    "telefono": "+56912345678",
    "patente": "ABCD12",
    "marca": "Toyota",
    "modelo": "Corolla",
    "anio": 2023,
    "color": "Blanco",
    "tipoVehiculo": "Sedan",
    "observaciones": "Primera solicitud de prueba"
  }'
```

**Respuesta esperada (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombrePropietario": "Juan Pérez",
  "rut": "12345678-9",
  "email": "juan.perez@example.com",
  "patente": "ABCD12",
  "marca": "Toyota",
  "modelo": "Corolla",
  "anio": 2023,
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-02-15T16:30:00"
}
```

### 3. Listar Solicitudes

```bash
curl http://localhost:8080/api/v1/solicitudes?page=0&size=10
```

### 4. Obtener Solicitud por ID

```bash
# Reemplazar {id} con el UUID obtenido
curl http://localhost:8080/api/v1/solicitudes/{id}
```

### 5. Generar URL Prefirmada para S3

```bash
# Reemplazar {solicitudId} con un UUID válido
curl -X POST "http://localhost:8080/api/v1/solicitudes/{solicitudId}/presigned-url?tipoDocumento=cedula"
```

**Respuesta esperada:**
```json
{
  "uploadUrl": "http://fleet-documents.s3.localhost.localstack.cloud:4566/solicitudes/...",
  "fileKey": "solicitudes/{solicitudId}/cedula_2026-02-15T16-30-00.pdf",
  "expiresAt": "2026-02-15T16:45:00",
  "message": "URL generada exitosamente. Válida por 15 minutos."
}
```

---

## 🔍 Verificación de Servicios AWS (LocalStack)

### Instalar AWS CLI Local (Opcional)

```bash
pip install awscli-local
```

### Verificar Bucket S3

```bash
# Listar buckets
awslocal s3 ls

# Listar contenido del bucket
awslocal s3 ls s3://fleet-documents/

# Ver detalles del bucket
awslocal s3api get-bucket-versioning --bucket fleet-documents
```

### Verificar Cola SQS

```bash
# Listar colas
awslocal sqs list-queues

# Obtener atributos de la cola
awslocal sqs get-queue-attributes \
  --queue-url http://localhost:4566/000000000000/fleet-solicitudes-queue \
  --attribute-names All
```

### Verificar Secrets Manager

```bash
# Listar secrets
awslocal secretsmanager list-secrets

# Obtener valor del secret
awslocal secretsmanager get-secret-value \
  --secret-id fleet-db-credentials
```

### Acceder a LocalStack UI (Opcional)

Si tienes LocalStack Pro, puedes acceder a:
```
http://localhost:4566/_localstack/health
```

---

## 🗄️ Acceso Directo a PostgreSQL

### Usando Docker

```bash
docker exec -it fleet-postgres psql -U postgres -d fleet_management
```

### Consultas Útiles

```sql
-- Ver todas las solicitudes
SELECT id, nombre_propietario, patente, estado, fecha_creacion 
FROM solicitudes 
ORDER BY fecha_creacion DESC;

-- Contar solicitudes por estado
SELECT estado, COUNT(*) 
FROM solicitudes 
GROUP BY estado;

-- Ver última solicitud creada
SELECT * FROM solicitudes 
ORDER BY fecha_creacion DESC 
LIMIT 1;
```

---

## 📊 Monitoreo y Logs

### Ver Logs en Tiempo Real

```bash
# Todos los servicios
docker-compose logs -f

# Solo aplicación Java
docker-compose logs -f app

# Solo PostgreSQL
docker-compose logs -f postgres

# Solo LocalStack
docker-compose logs -f localstack
```

### Ver Logs de Inicialización de LocalStack

```bash
docker-compose logs localstack | grep "init-aws-services"
```

---

## 🛑 Detener y Limpiar

### Detener Servicios

```bash
# Detener sin eliminar volúmenes
docker-compose down

# Detener y eliminar volúmenes (limpieza completa)
docker-compose down -v
```

### Limpiar Todo

```bash
# Eliminar contenedores, redes, volúmenes e imágenes
docker-compose down -v --rmi all

# Limpiar sistema Docker completo (cuidado)
docker system prune -a --volumes
```

---

## 🔧 Troubleshooting

### Problema: Puerto 8080 ya está en uso

**Solución:**
```bash
# Encontrar proceso usando el puerto
netstat -ano | findstr :8080

# Matar proceso (reemplazar PID)
taskkill /PID <PID> /F

# O cambiar puerto en docker-compose.yml
ports:
  - "8081:8080"  # Usar 8081 en lugar de 8080
```

### Problema: LocalStack no inicia correctamente

**Solución:**
```bash
# Ver logs detallados
docker-compose logs localstack

# Reiniciar solo LocalStack
docker-compose restart localstack

# Verificar health
curl http://localhost:4566/_localstack/health
```

### Problema: Aplicación no conecta a PostgreSQL

**Verificar:**
```bash
# 1. PostgreSQL está healthy
docker-compose ps postgres

# 2. Conectividad desde app
docker exec fleet-app ping postgres

# 3. Credenciales correctas en docker-compose.yml
```

### Problema: Error "Cannot find AWS credentials"

**Solución:**
El docker-compose ya configura las credenciales. Verificar:
```bash
docker-compose exec app env | grep AWS
```

Debe mostrar:
```
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
AWS_REGION=us-east-1
```

### Problema: S3 Bucket no existe

**Solución:**
```bash
# Verificar que el script de inicialización se ejecutó
docker-compose logs localstack | grep "fleet-documents"

# Crear manualmente si es necesario
awslocal s3 mb s3://fleet-documents
```

---

## 📝 Colección Postman

Puedes importar esta colección en Postman para pruebas más fáciles:

```json
{
  "info": {
    "name": "Fleet Management - Local",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Crear Solicitud",
      "request": {
        "method": "POST",
        "header": [{"key": "Content-Type", "value": "application/json"}],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"nombrePropietario\": \"Juan Pérez\",\n  \"rut\": \"12345678-9\",\n  \"email\": \"juan@example.com\",\n  \"telefono\": \"+56912345678\",\n  \"patente\": \"ABCD12\",\n  \"marca\": \"Toyota\",\n  \"modelo\": \"Corolla\",\n  \"anio\": 2023,\n  \"color\": \"Blanco\",\n  \"tipoVehiculo\": \"Sedan\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/v1/solicitudes",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "solicitudes"]
        }
      }
    },
    {
      "name": "Listar Solicitudes",
      "request": {
        "method": "GET",
        "url": {
          "raw": "http://localhost:8080/api/v1/solicitudes?page=0&size=10",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "v1", "solicitudes"],
          "query": [
            {"key": "page", "value": "0"},
            {"key": "size", "value": "10"}
          ]
        }
      }
    }
  ]
}
```

---

## 🎯 Casos de Prueba Recomendados

### 1. Validación de RUT
```bash
# RUT inválido (debe fallar)
curl -X POST http://localhost:8080/api/v1/solicitudes \
  -H "Content-Type: application/json" \
  -d '{"nombrePropietario":"Test","rut":"11111111-1",...}'
```

### 2. Validación de Patente
```bash
# Patente duplicada (debe fallar)
# Crear dos solicitudes con la misma patente
```

### 3. Paginación
```bash
# Crear 15 solicitudes y probar paginación
curl "http://localhost:8080/api/v1/solicitudes?page=0&size=5"
curl "http://localhost:8080/api/v1/solicitudes?page=1&size=5"
```

### 4. Ordenamiento
```bash
curl "http://localhost:8080/api/v1/solicitudes?sortBy=fechaCreacion&sortDir=DESC"
```

---

## 📚 Recursos Adicionales

- [LocalStack Documentation](https://docs.localstack.cloud/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/)

---

## ✅ Checklist de Verificación

Antes de considerar que todo funciona correctamente:

- [ ] PostgreSQL está healthy
- [ ] LocalStack está healthy
- [ ] Aplicación Java inició sin errores
- [ ] Bucket S3 `fleet-documents` existe
- [ ] Cola SQS `fleet-solicitudes-queue` existe
- [ ] Secret `fleet-db-credentials` existe
- [ ] Puedes crear una solicitud vía API
- [ ] Puedes listar solicitudes
- [ ] Puedes generar URL prefirmada
- [ ] Base de datos persiste datos entre reinicios

---

**Última actualización:** 2026-02-15  
**Versión:** 1.0.0  
**Entorno:** Local (Sin AWS)
