# 🚗 Fleet Management Platform - Prueba Técnica Backend

Sistema de gestión de solicitudes de inscripción de vehículos desarrollado con **Java 21**, **Spring Boot 3.5** y **AWS CDK**.

---

## 📋 Descripción

Microservicio RESTful que permite:
- ✅ Registrar solicitudes de inscripción de vehículos
- ✅ Validar datos (RUT, patente, año del vehículo)
- ✅ Generar URLs prefirmadas para subir documentos a S3
- ✅ Publicar eventos a SQS para procesamiento asíncrono
- ✅ Consultar solicitudes con paginación

---

## 🚀 Inicio Rápido

### Prerrequisitos

- **Java 21**
- **PostgreSQL 15+**
- **Docker** (opcional)

### Ejecutar Localmente

```bash
# 1. Clonar repositorio
git clone <repository-url>
cd arkho-prueba

# 2. Configurar PostgreSQL
createdb fleet_management

# 3. Ejecutar aplicación
cd vehicle-registration-service
./gradlew bootRun

# 4. Probar API
curl http://localhost:8080/api/v1/solicitudes
```

---

## 📚 Documentación Completa

### 📖 Guías Principales

| Documento | Descripción |
|-----------|-------------|
| **[📘 Guía de Usuario](vehicle-registration-service/README.md)** | Instalación, configuración, API, ejemplos completos |
| **[🏗️ Decisiones de Arquitectura](vehicle-registration-service/ARCHITECTURE.md)** | Justificaciones técnicas (PostgreSQL vs DynamoDB, ECS vs Lambda) |
| **[✅ Cumplimiento del Reto](vehicle-registration-service/CHALLENGE-COMPLIANCE.md)** | Verificación 100% de requerimientos |
| **[🧪 Pruebas Locales](vehicle-registration-service/LOCAL-TESTING.md)** | Testing sin AWS usando LocalStack |
| **[☁️ Despliegue AWS](cdkinfra/DEPLOYMENT.md)** | Infraestructura como código con CDK |

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                    API REST Layer                        │
│              (Spring Boot 3.5 + Java 21)                 │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Service Layer                            │
│         (Validaciones + Lógica de Negocio)              │
└────────────┬───────────────────────┬────────────────────┘
             │                       │
┌────────────▼──────────┐   ┌───────▼────────────────────┐
│   PostgreSQL (JPA)    │   │   AWS (S3 + SQS)           │
└───────────────────────┘   └────────────────────────────┘
```

**Decisiones Clave:**
- **PostgreSQL**: Validaciones ACID, relaciones complejas
- **ECS Fargate**: Sin cold starts, pool de conexiones persistente
- **S3 Presigned URLs**: Upload directo sin pasar por backend

📖 **Detalles:** Ver [ARCHITECTURE.md](vehicle-registration-service/ARCHITECTURE.md)

---

## 🔌 API REST

### Endpoints Principales

```bash
# Crear solicitud
POST /api/v1/solicitudes

# Listar solicitudes (con paginación)
GET /api/v1/solicitudes?page=0&size=10

# Obtener solicitud por ID
GET /api/v1/solicitudes/{id}

# Generar URL para subir documento
POST /api/v1/solicitudes/{id}/documentos/upload-url
```

### Ejemplo de Uso

```bash
# Crear solicitud
curl -X POST http://localhost:8080/api/v1/solicitudes \
  -H "Content-Type: application/json" \
  -d '{
    "nombrePropietario": "Juan Pérez",
    "rut": "12345678-5",
    "email": "juan.perez@example.com",
    "telefono": "+56912345678",
    "patente": "ABCD12",
    "marca": "Toyota",
    "modelo": "Corolla",
    "anio": 2023,
    "color": "Blanco",
    "tipoVehiculo": "Sedan"
  }'
```

📖 **Documentación completa de API:** Ver [README.md - Sección API](vehicle-registration-service/README.md#-api-rest)

---

## 🛠️ Tecnologías

| Componente | Tecnología | Versión |
|------------|------------|---------|
| **Backend** | Spring Boot | 3.5.0 |
| **Lenguaje** | Java | 21 |
| **Base de Datos** | PostgreSQL | 15+ |
| **Cloud** | AWS (S3, SQS, RDS, ECS) | SDK 2.23.9 |
| **IaC** | AWS CDK | TypeScript |
| **Testing** | JUnit 5 + Testcontainers | - |

---

## 📂 Estructura del Proyecto

```
arkho-prueba/
├── README.md                           # ← Este archivo
│
├── vehicle-registration-service/       # Microservicio Java
│   ├── README.md                       # Guía completa de usuario
│   ├── ARCHITECTURE.md                 # Decisiones técnicas
│   ├── CHALLENGE-COMPLIANCE.md         # Cumplimiento del reto
│   ├── LOCAL-TESTING.md                # Pruebas locales
│   ├── src/                            # Código fuente
│   ├── build.gradle                    # Configuración Gradle
│   ├── docker-compose.yml              # PostgreSQL + LocalStack
│   └── Dockerfile                      # Imagen Docker
│
└── cdkinfra/                           # Infraestructura AWS
    ├── DEPLOYMENT.md                   # Guía de despliegue
    ├── lib/infracdk-stack.ts           # Stack CDK
    └── bin/infracdk.ts                 # Entry point
```

---

## ✅ Características Principales

### Robustez
- ✅ **Idempotencia**: Prevención de duplicados con constraints de BD
- ✅ **Sanitización**: Trimming y normalización automática
- ✅ **Validaciones**: RUT chileno, patente, año del vehículo
- ✅ **Manejo de errores**: Sin stack traces expuestos al cliente

### Seguridad
- ✅ **Presigned URLs**: Upload directo a S3 (URLs temporales de 15 min)
- ✅ **Mínimo privilegio**: IAM roles restrictivos
- ✅ **S3 privado**: Sin acceso público

### Escalabilidad
- ✅ **Aurora Serverless v2**: Auto-escalado de 0.5 a 2 ACU
- ✅ **ECS Fargate**: Auto-scaling basado en CPU/memoria
- ✅ **Paginación**: Consultas eficientes con Spring Data

📖 **Detalles técnicos:** Ver [ARCHITECTURE.md](vehicle-registration-service/ARCHITECTURE.md)

---

## 🧪 Testing

```bash
# Tests unitarios
cd vehicle-registration-service
./gradlew test

# Tests de integración
./gradlew integrationTest

# Cobertura de código
./gradlew jacocoTestReport
```

**Cobertura:** 10 tests unitarios con JUnit 5 + Mockito

📖 **Guía de testing:** Ver [LOCAL-TESTING.md](vehicle-registration-service/LOCAL-TESTING.md)

---

## 🚢 Despliegue

### Opción 1: Docker Compose (Local)

```bash
cd vehicle-registration-service
docker compose up --build -d
```

### Opción 2: AWS con CDK

```bash
cd cdkinfra
npm install
cdk deploy
```

📖 **Guía completa de despliegue:** Ver [DEPLOYMENT.md](cdkinfra/DEPLOYMENT.md)

---

## 📊 Cumplimiento de Requerimientos

| Categoría | Estado |
|-----------|--------|
| **API REST** | ✅ 4/4 endpoints |
| **Validaciones** | ✅ Patente, RUT, año |
| **Persistencia** | ✅ PostgreSQL + JPA |
| **Asincronía** | ✅ SQS + eventos |
| **Infraestructura CDK** | ✅ VPC, RDS, ECS, S3, SQS |
| **Robustez** | ✅ Idempotencia, sanitización |
| **Tests** | ✅ 10 tests unitarios |
| **Documentación** | ✅ 5 archivos MD |

**Total:** ✅ **22/22 requerimientos (100%)**

📖 **Verificación detallada:** Ver [CHALLENGE-COMPLIANCE.md](vehicle-registration-service/CHALLENGE-COMPLIANCE.md)

---

## 🔧 Troubleshooting

### Problemas Comunes

**Error: Connection refused a PostgreSQL**
```bash
# Verificar que PostgreSQL esté corriendo
sudo systemctl status postgresql

# Verificar puerto
netstat -an | grep 5432
```

**Error: Bean SqsClient no encontrado**
```yaml
# Deshabilitar SQS en application.yaml
aws:
  sqs:
    enabled: false
```

📖 **Más soluciones:** Ver [README.md - Troubleshooting](vehicle-registration-service/README.md#-troubleshooting)

---

## 👥 Contribución

```bash
# 1. Fork del proyecto
# 2. Crear rama feature
git checkout -b feature/nueva-funcionalidad

# 3. Commit con Conventional Commits
git commit -m "feat: agregar validación de VIN"

# 4. Push y Pull Request
git push origin feature/nueva-funcionalidad
```

---

## 📄 Licencia

Este proyecto es una prueba técnica para demostración de habilidades.

---

## 📞 Contacto

Para preguntas sobre la implementación, revisar la documentación detallada:

- **Instalación y uso:** [vehicle-registration-service/README.md](vehicle-registration-service/README.md)
- **Decisiones técnicas:** [vehicle-registration-service/ARCHITECTURE.md](vehicle-registration-service/ARCHITECTURE.md)
- **Cumplimiento del reto:** [vehicle-registration-service/CHALLENGE-COMPLIANCE.md](vehicle-registration-service/CHALLENGE-COMPLIANCE.md)

---

**Versión:** 1.0.0  
**Última actualización:** 2026-02-16
