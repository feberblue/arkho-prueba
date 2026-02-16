# 🚗 Vehicle Registration Service

Microservicio RESTful para gestión de solicitudes de inscripción de vehículos.

**Stack:** Spring Boot 3.5 + Java 21 + PostgreSQL + AWS (S3, SQS)

---

## 📖 Documentación

| Documento | Descripción |
|-----------|-------------|
| **[🏠 README Principal](../README.md)** | Inicio rápido del proyecto completo |
| **[🏗️ Arquitectura](ARCHITECTURE.md)** | Decisiones técnicas (PostgreSQL vs DynamoDB, ECS vs Lambda) |
| **[✅ Cumplimiento](CHALLENGE-COMPLIANCE.md)** | Verificación 100% de requerimientos del reto |
| **[🧪 Testing Local](LOCAL-TESTING.md)** | Pruebas sin AWS usando LocalStack |
| **[☁️ Despliegue AWS](../cdkinfra/DEPLOYMENT.md)** | Infraestructura como código con CDK |

---

## 🚀 Inicio Rápido

### Prerrequisitos

- Java 21
- PostgreSQL 15+
- Gradle (incluido)

### Ejecutar

```bash
# 1. Crear base de datos
createdb fleet_management

# 2. Ejecutar aplicación
./gradlew bootRun

# 3. Probar API
curl http://localhost:8080/api/v1/solicitudes
```

---

## 📡 API REST

### Endpoints

```bash
# Crear solicitud
POST /api/v1/solicitudes

# Listar con paginación
GET /api/v1/solicitudes?page=0&size=10

# Obtener por ID
GET /api/v1/solicitudes/{id}

# Generar URL de upload
POST /api/v1/solicitudes/{id}/documentos/upload-url
```

### Ejemplo

```bash
curl -X POST http://localhost:8080/api/v1/solicitudes \
  -H "Content-Type: application/json" \
  -d '{
    "nombrePropietario": "Juan Pérez",
    "rut": "12345678-5",
    "email": "juan@example.com",
    "telefono": "+56912345678",
    "patente": "ABCD12",
    "marca": "Toyota",
    "modelo": "Corolla",
    "anio": 2023
  }'
```

**Respuesta:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "patente": "ABCD12",
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-02-16T10:00:00"
}
```

---

## ✅ Validaciones

- **RUT chileno:** Formato `12345678-9` con dígito verificador
- **Patente:** Formatos `ABCD12` o `AB1234` (chilena)
- **Año:** Entre 1900 y año actual + 1
- **Email:** Formato válido
- **Patente única:** No se permiten duplicados

---

## 🧪 Testing

```bash
# Tests unitarios
./gradlew test

# Tests de integración
./gradlew integrationTest

# Cobertura
./gradlew jacocoTestReport
```

**Cobertura:** 10 tests unitarios (JUnit 5 + Mockito)

---

## 🐳 Docker

### Opción 1: Docker Compose

```bash
docker compose up --build -d
```

### Opción 2: Dockerfile

```bash
docker build -t fleet-app .
docker run -p 8080:8080 fleet-app
```

---

## 🔧 Configuración

### Variables de Entorno

```bash
# Base de datos
DB_URL=jdbc:postgresql://localhost:5432/fleet_management
DB_USERNAME=postgres
DB_PASSWORD=Admin123

# AWS (opcional)
AWS_S3_ENABLED=false
AWS_SQS_ENABLED=false
```

---

## 🔍 Troubleshooting

### Error: Connection refused a PostgreSQL

```bash
# Verificar que PostgreSQL esté corriendo
sudo systemctl status postgresql

# Verificar puerto
netstat -an | grep 5432
```

### Error: Bean SqsClient no encontrado

```yaml
# Deshabilitar SQS en application.yaml
aws:
  sqs:
    enabled: false
```

📖 **Más soluciones:** Ver documentación completa en los enlaces arriba.

---

## 📚 Recursos Adicionales

- **[Instalación detallada](../README.md#-inicio-rápido)** - Guía paso a paso desde cero
- **[Justificaciones técnicas](ARCHITECTURE.md)** - Por qué PostgreSQL, ECS Fargate, etc.
- **[Pruebas locales](LOCAL-TESTING.md)** - Testing con LocalStack
- **[Despliegue AWS](../cdkinfra/DEPLOYMENT.md)** - Infraestructura CDK completa

---

**Versión:** 1.0.0  
**Última actualización:** 2026-02-16
