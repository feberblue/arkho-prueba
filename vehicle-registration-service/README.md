# 🚗 Vehicle Registration Service - Fleet Management

Sistema de gestión de solicitudes de inscripción de vehículos para flotas empresariales, desarrollado con **Spring Boot 3.5** y **Java 21**.

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Arquitectura](#-arquitectura)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación desde Cero](#-instalación-desde-cero)
- [Configuración](#-configuración)
- [Ejecución](#-ejecución)
- [API REST](#-api-rest)
- [Validaciones](#-validaciones)
- [Testing](#-testing)
- [Despliegue](#-despliegue)
- [Troubleshooting](#-troubleshooting)
- [Contribución](#-contribución)

---

## 🎯 Descripción

**Vehicle Registration Service** es un microservicio RESTful que gestiona el ciclo de vida completo de solicitudes de inscripción de vehículos para flotas empresariales. Permite crear, consultar, actualizar y eliminar solicitudes, con validaciones robustas y soporte para integración con servicios AWS (S3 para documentos y SQS para eventos).

### Funcionalidades Principales

- ✅ **CRUD completo** de solicitudes de inscripción
- ✅ **Validaciones chilenas** (RUT, patente)
- ✅ **Generación de URLs prefirmadas** para carga de documentos en S3
- ✅ **Publicación de eventos** a SQS cuando se crea una solicitud
- ✅ **Paginación y ordenamiento** de resultados
- ✅ **Auditoría automática** con timestamps
- ✅ **Manejo de errores** centralizado y estructurado
- ✅ **Health checks** con Spring Boot Actuator

---

## ✨ Características

### Técnicas

- **Java 21** con características modernas
- **Spring Boot 3.5** (última versión estable)
- **Spring Data JPA** con Hibernate
- **PostgreSQL** como base de datos
- **Lombok** para reducir boilerplate
- **Bean Validation** para validaciones declarativas
- **AWS SDK v2** para S3 y SQS
- **Docker** y **Docker Compose** para contenedorización
- **Gradle** como herramienta de construcción

### Funcionales

- Validación de RUT chileno con dígito verificador
- Validación de patente chilena (formatos LLLL12 y LL1234)
- Prevención de duplicados por patente
- Generación de URLs prefirmadas S3 con expiración de 15 minutos
- Publicación asíncrona de eventos a SQS
- Modo simulación para desarrollo sin AWS

---

## 🛠️ Tecnologías

| Categoría | Tecnología | Versión |
|-----------|-----------|---------|
| **Lenguaje** | Java | 21 |
| **Framework** | Spring Boot | 3.5.10 |
| **Build Tool** | Gradle | 8.x |
| **Base de Datos** | PostgreSQL | 15+ |
| **ORM** | Hibernate (JPA) | 6.x |
| **Cloud** | AWS SDK (S3, SQS) | 2.23.9 |
| **Contenedores** | Docker | 20.10+ |
| **Testing** | JUnit 5, Testcontainers | - |

---

## 🏗️ Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                    API REST Layer                        │
│              (SolicitudController)                       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                 Service Layer                            │
│    (SolicitudService, PresignedUrlService)              │
└────────────┬───────────────────────┬────────────────────┘
             │                       │
┌────────────▼──────────┐   ┌───────▼────────────────────┐
│   Repository Layer    │   │   Event Publisher          │
│ (SolicitudRepository) │   │  (EventPublisher)          │
└────────────┬──────────┘   └───────┬────────────────────┘
             │                       │
┌────────────▼──────────┐   ┌───────▼────────────────────┐
│    PostgreSQL DB      │   │      AWS SQS               │
└───────────────────────┘   └────────────────────────────┘
```

### Estructura de Paquetes

```
com.management.registration
├── config/              # Configuración (AWS, CORS, etc.)
├── controller/          # Controladores REST
├── dto/                 # DTOs (Request/Response)
│   ├── request/
│   └── response/
├── entity/              # Entidades JPA
├── event/               # Publicación de eventos
├── exception/           # Excepciones personalizadas
├── repository/          # Repositorios JPA
├── service/             # Lógica de negocio
└── validator/           # Validadores personalizados
```

---

## 📦 Requisitos Previos

### Obligatorios

1. **Java 21** (JDK)
   ```bash
   java -version
   # Debe mostrar: java version "21.x.x"
   ```

2. **PostgreSQL 15+**
   - Instalación local o contenedor Docker
   - Puerto por defecto: `5432`

3. **Gradle 8.x** (incluido en el proyecto vía wrapper)
   ```bash
   ./gradlew --version
   ```

### Opcionales (para entorno completo)

4. **Docker Desktop** (para LocalStack y entorno completo)
   ```bash
   docker --version
   docker compose version
   ```

5. **AWS CLI** (para pruebas con LocalStack)
   ```bash
   pip install awscli-local
   ```

---

## 🚀 Instalación desde Cero

### Paso 1: Clonar el Repositorio

```bash
git clone <repository-url>
cd vehicle-registration-service
```

### Paso 2: Configurar PostgreSQL

#### Opción A: PostgreSQL Local

1. **Instalar PostgreSQL**
   - Windows: Descargar desde [postgresql.org](https://www.postgresql.org/download/windows/)
   - Linux: `sudo apt-get install postgresql-15`
   - macOS: `brew install postgresql@15`

2. **Crear Base de Datos**
   ```sql
   -- Conectarse a PostgreSQL
   psql -U postgres
   
   -- Crear base de datos
   CREATE DATABASE fleet_management;
   
   -- Crear usuario (opcional)
   CREATE USER fleet_user WITH PASSWORD 'Admin123';
   GRANT ALL PRIVILEGES ON DATABASE fleet_management TO fleet_user;
   ```

3. **Verificar Conexión**
   ```bash
   psql -U postgres -d fleet_management -c "SELECT version();"
   ```

#### Opción B: PostgreSQL con Docker

```bash
docker run -d \
  --name fleet-postgres \
  -e POSTGRES_DB=fleet_management \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=Admin123 \
  -p 5432:5432 \
  postgres:15-alpine
```

### Paso 3: Configurar Variables de Entorno (Opcional)

Crear archivo `.env` en la raíz del proyecto:

```env
# Database
DB_URL=jdbc:postgresql://localhost:5432/fleet_management
DB_USERNAME=postgres
DB_PASSWORD=Admin123

# Server
PORT=8080

# AWS (Opcional - para producción)
AWS_REGION=us-east-1
AWS_S3_ENABLED=false
AWS_S3_BUCKET=fleet-documents
AWS_SQS_ENABLED=false
AWS_SQS_QUEUE_URL=
```

### Paso 4: Compilar el Proyecto

```bash
# Limpiar y compilar
./gradlew clean build -x test

# O con tests
./gradlew clean build
```

**Tiempo estimado:** 30-60 segundos

### Paso 5: Verificar Compilación

```bash
# Verificar que el JAR se generó
ls build/libs/

# Debe mostrar:
# vehicle-registration-service-0.0.1-SNAPSHOT.jar
```

---

## ⚙️ Configuración

### application.yaml

El archivo `src/main/resources/application.yaml` contiene toda la configuración:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/fleet_management}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:Admin123}
  
  jpa:
    hibernate:
      ddl-auto: update  # Crea/actualiza tablas automáticamente
    show-sql: false

server:
  port: ${PORT:8080}

aws:
  s3:
    enabled: ${AWS_S3_ENABLED:false}  # false = modo simulación
    bucket-name: ${AWS_S3_BUCKET:fleet-documents}
  
  sqs:
    enabled: ${AWS_SQS_ENABLED:false}  # false = modo simulación
    queue-url: ${AWS_SQS_QUEUE_URL:}
```

### Perfiles de Spring

- **Por defecto**: Modo desarrollo sin AWS
- **local**: Igual al por defecto
- **prod**: Para producción con AWS real

---

## 🎮 Ejecución

### Método 1: Gradle (Recomendado para Desarrollo)

```bash
# Ejecutar directamente
./gradlew bootRun

# Con perfil específico
./gradlew bootRun --args='--spring.profiles.active=local'
```

**La aplicación estará disponible en:** `http://localhost:8080`

### Método 2: JAR Ejecutable

```bash
# Compilar
./gradlew clean build -x test

# Ejecutar
java -jar build/libs/vehicle-registration-service-0.0.1-SNAPSHOT.jar
```

### Método 3: Docker Compose (Entorno Completo)

```bash
# Levantar PostgreSQL + LocalStack + App
docker compose up --build -d

# Ver logs
docker compose logs -f

# Detener
docker compose down
```

**Servicios incluidos:**
- PostgreSQL: `localhost:5432`
- LocalStack (S3/SQS): `localhost:4566`
- API REST: `localhost:8080`

### Método 4: Script PowerShell (Windows)

```powershell
.\start-local.ps1
```

Este script:
1. Verifica requisitos (Docker, puertos)
2. Limpia contenedores anteriores
3. Levanta todos los servicios
4. Valida que la API esté respondiendo

---

## 📡 API REST

### Base URL

```
http://localhost:8080/api/v1
```

### Endpoints Principales

#### 1. Crear Solicitud

**POST** `/solicitudes`

```bash
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
    "tipoVehiculo": "Sedan",
    "observaciones": "Primera solicitud"
  }'
```

**PowerShell:**
```powershell
$body = @{
    nombrePropietario = "Juan Pérez"
    rut = "12345678-5"
    email = "juan.perez@example.com"
    telefono = "+56912345678"
    patente = "ABCD12"
    marca = "Toyota"
    modelo = "Corolla"
    anio = 2023
    color = "Blanco"
    tipoVehiculo = "Sedan"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/v1/solicitudes" `
  -Method POST -Body $body -ContentType "application/json"
```

**Respuesta (201 Created):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "nombrePropietario": "Juan Pérez",
  "rut": "123456785",
  "email": "juan.perez@example.com",
  "telefono": "+56912345678",
  "patente": "ABCD12",
  "marca": "Toyota",
  "modelo": "Corolla",
  "anio": 2023,
  "color": "Blanco",
  "tipoVehiculo": "Sedan",
  "estado": "PENDIENTE",
  "observaciones": "Primera solicitud",
  "fechaCreacion": "2026-02-15T14:30:00",
  "fechaActualizacion": "2026-02-15T14:30:00"
}
```

#### 2. Listar Solicitudes (Paginado)

**GET** `/solicitudes?page=0&size=10&sortBy=fechaCreacion&sortDir=DESC`

```bash
curl "http://localhost:8080/api/v1/solicitudes?page=0&size=10"
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/solicitudes?page=0&size=10"
```

**Respuesta (200 OK):**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "nombrePropietario": "Juan Pérez",
      "patente": "ABCD12",
      "estado": "PENDIENTE",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

#### 3. Obtener Solicitud por ID

**GET** `/solicitudes/{id}`

```bash
curl http://localhost:8080/api/v1/solicitudes/550e8400-e29b-41d4-a716-446655440000
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/solicitudes/550e8400-e29b-41d4-a716-446655440000"
```

#### 4. Actualizar Solicitud

**PUT** `/solicitudes/{id}`

```bash
curl -X PUT http://localhost:8080/api/v1/solicitudes/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "nombrePropietario": "Juan Pérez Actualizado",
    "rut": "12345678-5",
    "email": "juan.nuevo@example.com",
    "telefono": "+56912345678",
    "patente": "ABCD12",
    "marca": "Toyota",
    "modelo": "Corolla",
    "anio": 2023,
    "color": "Negro",
    "tipoVehiculo": "Sedan"
  }'
```

#### 5. Eliminar Solicitud

**DELETE** `/solicitudes/{id}`

```bash
curl -X DELETE http://localhost:8080/api/v1/solicitudes/550e8400-e29b-41d4-a716-446655440000
```

#### 6. Generar URL Prefirmada para S3

**POST** `/solicitudes/{id}/presigned-url?tipoDocumento=cedula`

```bash
curl -X POST "http://localhost:8080/api/v1/solicitudes/550e8400-e29b-41d4-a716-446655440000/presigned-url?tipoDocumento=cedula"
```

**Tipos de documento válidos:**
- `cedula`
- `licencia`
- `revision_tecnica`
- `seguro`
- `contrato`
- `otro`

**Respuesta (200 OK):**
```json
{
  "uploadUrl": "https://fleet-documents.s3.amazonaws.com/solicitudes/...",
  "fileKey": "solicitudes/550e8400.../cedula_2026-02-15.pdf",
  "expiresAt": "2026-02-15T14:45:00",
  "message": "URL generada exitosamente. Válida por 15 minutos."
}
```

#### 7. Health Check

**GET** `/actuator/health`

```bash
curl http://localhost:8080/actuator/health
```

**Respuesta:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

### Parámetros de Paginación

| Parámetro | Tipo | Por Defecto | Descripción |
|-----------|------|-------------|-------------|
| `page` | Integer | 0 | Número de página (base 0) |
| `size` | Integer | 10 | Elementos por página |
| `sortBy` | String | fechaCreacion | Campo para ordenar |
| `sortDir` | String | ASC | Dirección (ASC/DESC) |

**Campos ordenables:**
- `fechaCreacion`
- `fechaActualizacion`
- `nombrePropietario`
- `patente`
- `estado`

---

## ✅ Validaciones

### RUT Chileno

- **Formato:** `12345678-9` (8 dígitos + guión + dígito verificador)
- **Validación:** Algoritmo módulo 11
- **Ejemplo válido:** `12345678-5`

### Patente Chilena

- **Formato 1:** `LLLL12` (4 letras + 2 números)
- **Formato 2:** `LL1234` (2 letras + 4 números)
- **Ejemplos válidos:** `ABCD12`, `XY9876`

### Email

- **Formato:** RFC 5322
- **Ejemplo:** `usuario@dominio.com`

### Teléfono

- **Formato:** Internacional con `+`
- **Ejemplo:** `+56912345678`

### Año del Vehículo

- **Rango:** 1900 - (año actual + 1)
- **Ejemplo:** Para 2026, válido: 1900-2027

### Campos Obligatorios

- `nombrePropietario`
- `rut`
- `email`
- `telefono`
- `patente`
- `marca`
- `modelo`
- `anio`
- `color`
- `tipoVehiculo`

---

## 🧪 Testing

### Ejecutar Tests Unitarios

```bash
./gradlew test
```

### Ejecutar Tests de Integración

```bash
./gradlew integrationTest
```

### Cobertura de Código

```bash
./gradlew jacocoTestReport

# Ver reporte
open build/reports/jacoco/test/html/index.html
```

### Tests con Testcontainers

Los tests de integración usan **Testcontainers** para levantar PostgreSQL automáticamente:

```java
@Testcontainers
@SpringBootTest
class SolicitudServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    
    // Tests...
}
```

---

## 🚢 Despliegue

### Opción 1: Docker

```bash
# Construir imagen
docker build -t vehicle-registration-service:latest .

# Ejecutar contenedor
docker run -d \
  -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/fleet_management \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=Admin123 \
  --name fleet-app \
  vehicle-registration-service:latest
```

### Opción 2: AWS (con CDK)

El proyecto incluye infraestructura como código en `../cdkinfra/`:

```bash
cd ../cdkinfra

# Instalar dependencias
npm install

# Desplegar
cdk deploy
```

**Recursos creados:**
- VPC con 2 AZs
- Aurora PostgreSQL Serverless v2
- ECS Fargate con ALB
- S3 Bucket privado
- SQS Queue
- Secrets Manager

Ver documentación completa en [`../cdkinfra/DEPLOYMENT.md`](../cdkinfra/DEPLOYMENT.md)

### Opción 3: JAR en Servidor

```bash
# En el servidor
java -jar vehicle-registration-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --DB_URL=jdbc:postgresql://prod-db:5432/fleet_management \
  --DB_USERNAME=fleet_user \
  --DB_PASSWORD=secure_password
```

---

## 🔧 Troubleshooting

### Error: "Failed to determine a suitable driver class"

**Causa:** Spring no encuentra el driver de PostgreSQL

**Solución:**
```bash
# Verificar que PostgreSQL esté en build.gradle
./gradlew dependencies | grep postgresql

# Limpiar y recompilar
./gradlew clean build --refresh-dependencies
```

### Error: "Connection refused" al conectar a PostgreSQL

**Causa:** PostgreSQL no está corriendo o puerto incorrecto

**Solución:**
```bash
# Verificar que PostgreSQL esté corriendo
# Windows
netstat -ano | findstr :5432

# Linux/Mac
lsof -i :5432

# Iniciar PostgreSQL si está detenido
# Windows (como servicio)
net start postgresql-x64-15

# Linux
sudo systemctl start postgresql

# Mac
brew services start postgresql@15
```

### Error: "Bean 'sqsClient' could not be found"

**Causa:** SQS está habilitado pero no hay configuración AWS

**Solución:**
```yaml
# En application.yaml, asegurar:
aws:
  sqs:
    enabled: false  # Cambiar a false para desarrollo local
```

### Error: "RUT inválido"

**Causa:** El RUT no tiene el formato correcto o dígito verificador inválido

**Solución:**
```
Formato correcto: 12345678-5
- 8 dígitos
- Guión
- 1 dígito verificador (puede ser 0-9 o K)
```

Usar RUTs válidos para pruebas:
- `12345678-5`
- `11111111-1`
- `22222222-2`

### Error: "Patente ya registrada"

**Causa:** Ya existe una solicitud con esa patente

**Solución:**
```bash
# Usar una patente diferente
# O eliminar la solicitud existente
curl -X DELETE http://localhost:8080/api/v1/solicitudes/{id}
```

### Puerto 8080 ya en uso

**Causa:** Otra aplicación está usando el puerto 8080

**Solución:**
```bash
# Cambiar puerto en application.yaml
server:
  port: 8081

# O usar variable de entorno
PORT=8081 ./gradlew bootRun
```

### Logs no aparecen

**Causa:** Nivel de log muy alto

**Solución:**
```yaml
# En application.yaml
logging:
  level:
    root: INFO
    com.management.registration: DEBUG
```

---

## 📚 Documentación Adicional

### Archivos de Documentación

- **[LOCAL-TESTING.md](LOCAL-TESTING.md)** - Guía completa de pruebas locales sin AWS
- **[../cdkinfra/DEPLOYMENT.md](../cdkinfra/DEPLOYMENT.md)** - Guía de despliegue en AWS
- **[Dockerfile](Dockerfile)** - Configuración de contenedor Docker
- **[docker-compose.yml](docker-compose.yml)** - Orquestación de servicios

### Scripts Útiles

- **[start-local.ps1](start-local.ps1)** - Script de inicio automático (Windows)
- **[localstack-init/](localstack-init/)** - Scripts de inicialización de LocalStack

### Recursos Externos

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [AWS SDK for Java](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Lombok](https://projectlombok.org/)

---

## 🤝 Contribución

### Estándares de Código

- **Java 21** con características modernas
- **Convenciones de nombres:**
  - Variables/métodos: `camelCase`
  - Clases: `PascalCase`
  - Constantes: `UPPER_SNAKE_CASE`
- **Lombok** para reducir boilerplate
- **Validaciones** con Bean Validation
- **Manejo de errores** centralizado

### Commits

Usar [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(solicitudes): agregar endpoint de búsqueda por patente
fix(validation): corregir validación de RUT
docs(readme): actualizar instrucciones de instalación
test(service): agregar tests para SolicitudService
```

### Pull Requests

1. Fork del repositorio
2. Crear rama feature: `git checkout -b feature/nueva-funcionalidad`
3. Commit cambios: `git commit -m 'feat: agregar nueva funcionalidad'`
4. Push a la rama: `git push origin feature/nueva-funcionalidad`
5. Crear Pull Request

---

## 📄 Licencia

Este proyecto está bajo la licencia MIT. Ver archivo [LICENSE](LICENSE) para más detalles.

---

## 👥 Autores

- **Equipo de Desarrollo Fleet Management**

---

## 📞 Soporte

Para reportar bugs o solicitar nuevas funcionalidades, crear un issue en el repositorio.

---

## 🎯 Roadmap

- [ ] Autenticación y autorización con JWT
- [ ] Integración con sistema de notificaciones
- [ ] Dashboard de métricas con Grafana
- [ ] API GraphQL
- [ ] Webhooks para eventos
- [ ] Exportación de reportes (PDF, Excel)
- [ ] Búsqueda avanzada con Elasticsearch

---

**Última actualización:** 2026-02-15  
**Versión:** 0.0.1-SNAPSHOT
