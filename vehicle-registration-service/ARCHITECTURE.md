# 🏗️ Decisiones de Arquitectura - Fleet Management

## 📋 Índice

- [Resumen Ejecutivo](#resumen-ejecutivo)
- [Decisiones de Backend](#decisiones-de-backend)
- [Decisiones de Infraestructura (AWS CDK)](#decisiones-de-infraestructura-aws-cdk)
- [Seguridad y Robustez](#seguridad-y-robustez)
- [Justificaciones Técnicas](#justificaciones-técnicas)

---

## 🎯 Resumen Ejecutivo

Este documento detalla las decisiones arquitectónicas tomadas para implementar el microservicio de **Gestión de Solicitudes de Inscripción de Vehículos**, cumpliendo con los requerimientos de la prueba técnica.

### Decisiones Clave

| Aspecto | Decisión | Justificación |
|---------|----------|---------------|
| **Base de Datos** | PostgreSQL (RDS Aurora) | Modelo relacional, ACID, validaciones complejas |
| **Computación** | ECS Fargate | Balance costo/flexibilidad, Spring Boot optimizado |
| **Mensajería** | SQS | Desacoplamiento, retry automático, escalabilidad |
| **Almacenamiento** | S3 con Presigned URLs | Seguridad, escalabilidad, costo-efectivo |
| **Framework** | Spring Boot 3.5 | Ecosistema maduro, productividad, Java 21 |

---

## 🔧 Decisiones de Backend

### 1. Base de Datos: PostgreSQL vs DynamoDB

**Decisión:** ✅ **PostgreSQL (Aurora Serverless v2)**

#### Justificación

**Ventajas de PostgreSQL para este caso de uso:**

1. **Modelo Relacional Natural**
   - Las solicitudes tienen relaciones claras (propietario → vehículo → documentos)
   - Necesidad de JOINs para consultas complejas
   - Integridad referencial nativa

2. **Validaciones a Nivel de Base de Datos**
   ```sql
   -- Constraint de unicidad para patente
   ALTER TABLE solicitudes ADD CONSTRAINT uk_patente UNIQUE (patente);
   
   -- Validación de año
   ALTER TABLE solicitudes ADD CONSTRAINT chk_anio 
     CHECK (anio >= 1900 AND anio <= EXTRACT(YEAR FROM CURRENT_DATE) + 1);
   ```

3. **ACID Completo**
   - Transacciones críticas para evitar duplicados
   - Bloqueos optimistas con `@Version` en JPA
   - Rollback automático en caso de error

4. **Consultas Complejas**
   ```sql
   -- Búsqueda por múltiples criterios
   SELECT * FROM solicitudes 
   WHERE estado = 'PENDIENTE' 
     AND fecha_creacion > '2026-01-01'
     AND marca IN ('Toyota', 'Honda')
   ORDER BY fecha_creacion DESC;
   ```

5. **Ecosistema Spring**
   - Spring Data JPA maduro y probado
   - Hibernate con optimizaciones automáticas
   - Migraciones con Flyway/Liquibase

**Por qué NO DynamoDB en este caso:**

| Aspecto | DynamoDB | PostgreSQL |
|---------|----------|------------|
| **Consultas complejas** | ❌ Limitado a PK/SK | ✅ SQL completo |
| **Transacciones** | ⚠️ Limitadas | ✅ ACID completo |
| **Relaciones** | ❌ Desnormalización | ✅ Nativo |
| **Validaciones** | ❌ En aplicación | ✅ En BD |
| **Costo inicial** | ✅ Bajo | ⚠️ Medio |
| **Escalabilidad** | ✅ Infinita | ⚠️ Vertical |

**Conclusión:** Para un sistema de gestión con validaciones complejas, relaciones y consultas variadas, PostgreSQL es superior.

---

### 2. Computación: ECS Fargate vs Lambda

**Decisión:** ✅ **ECS Fargate**

#### Justificación

**Ventajas de ECS Fargate:**

1. **Spring Boot Optimizado**
   - Spring Boot está diseñado para ejecución continua
   - Warm-up de conexiones DB (HikariCP)
   - Caché de beans y contexto de aplicación

2. **Latencia Predecible**
   - Sin cold starts
   - Tiempo de respuesta consistente: 50-200ms
   - Ideal para APIs síncronas

3. **Gestión de Estado**
   - Conexiones persistentes a PostgreSQL
   - Pool de conexiones optimizado
   - Sesiones HTTP si es necesario

4. **Debugging y Monitoreo**
   - Logs estructurados en CloudWatch
   - Acceso directo al contenedor
   - Métricas detalladas con Container Insights

5. **Costo Predecible**
   ```
   ECS Fargate (0.5 vCPU, 1GB RAM):
   - $0.04048/hora = ~$30/mes
   - Costo fijo, fácil de presupuestar
   ```

**Lambda con SnapStart (Considerado pero NO elegido):**

| Aspecto | Lambda + SnapStart | ECS Fargate |
|---------|-------------------|-------------|
| **Cold Start** | ~1-2s (con SnapStart) | ❌ N/A (siempre warm) |
| **Warm Start** | ~100ms | ✅ 50-200ms |
| **Conexiones DB** | ⚠️ Limitadas (RDS Proxy) | ✅ Pool persistente |
| **Timeout** | ⚠️ 15 min máx | ✅ Ilimitado |
| **Costo bajo carga** | ⚠️ Variable | ✅ Fijo |
| **Spring Boot** | ⚠️ Overhead alto | ✅ Optimizado |

**¿Cuándo usar Lambda + SnapStart?**
- Tráfico muy esporádico (< 100 req/día)
- Funciones pequeñas y específicas
- Presupuesto muy limitado
- No requiere conexiones persistentes

**Conclusión:** Para una API REST con Spring Boot y PostgreSQL, ECS Fargate ofrece mejor rendimiento y simplicidad operacional.

---

### 3. Arquitectura de Capas

**Decisión:** ✅ **Clean Architecture (Controller → Service → Repository)**

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers, DTOs, Exception Handler) │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│          Business Layer                 │
│  (Services, Validators, Event Publisher)│
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│         Persistence Layer               │
│    (Repositories, Entities, JPA)        │
└─────────────────────────────────────────┘
```

#### Beneficios

1. **Separación de Responsabilidades**
   - Controllers: Solo HTTP (request/response)
   - Services: Lógica de negocio pura
   - Repositories: Acceso a datos

2. **Testabilidad**
   ```java
   @Test
   void crearSolicitud_conPatenteValida_debeRetornarSolicitud() {
       // Arrange
       when(repository.existsByPatente("ABCD12")).thenReturn(false);
       
       // Act
       SolicitudResponse response = service.crearSolicitud(request);
       
       // Assert
       assertNotNull(response.getId());
   }
   ```

3. **DTOs para Desacoplamiento**
   - `CrearSolicitudRequest`: Entrada del cliente
   - `SolicitudResponse`: Salida al cliente
   - `Solicitud` (Entity): Modelo de persistencia

4. **Manejo Centralizado de Errores**
   ```java
   @RestControllerAdvice
   public class GlobalExceptionHandler {
       @ExceptionHandler(PatenteYaRegistradaException.class)
       public ResponseEntity<ErrorResponse> handlePatenteYaRegistrada(
           PatenteYaRegistradaException ex) {
           // No expone stack traces
           return ResponseEntity.status(CONFLICT)
               .body(new ErrorResponse("PATENTE_DUPLICADA", ex.getMessage()));
       }
   }
   ```

---

## ☁️ Decisiones de Infraestructura (AWS CDK)

### 1. VPC: Diseño de Red

**Decisión:** ✅ **VPC con 2 AZs, subnets públicas y privadas**

```typescript
const vpc = new ec2.Vpc(this, 'FleetVPC', {
  maxAzs: 2,              // Alta disponibilidad
  natGateways: 1,         // Balance costo/disponibilidad
  subnetConfiguration: [
    {
      name: 'Public',
      subnetType: ec2.SubnetType.PUBLIC,
      cidrMask: 24
    },
    {
      name: 'Private',
      subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS,
      cidrMask: 24
    }
  ]
});
```

#### Justificación

- **2 AZs**: Tolerancia a fallos (si 1 AZ cae, la otra sigue)
- **Subnets privadas**: ECS y RDS no expuestos a internet
- **1 NAT Gateway**: Suficiente para desarrollo, escalable a 2 en producción
- **ALB en subnet pública**: Único punto de entrada

---

### 2. Base de Datos: Aurora Serverless v2

**Decisión:** ✅ **Aurora PostgreSQL Serverless v2**

```typescript
const db = new rds.DatabaseCluster(this, 'SolicitudesDB', {
  engine: rds.DatabaseClusterEngine.auroraPostgres({
    version: rds.AuroraPostgresEngineVersion.VER_15_4
  }),
  serverlessV2MinCapacity: 0.5,  // Escala desde 0.5 ACU
  serverlessV2MaxCapacity: 2,    // Hasta 2 ACU
  writer: rds.ClusterInstance.serverlessV2('writer'),
  readers: [
    rds.ClusterInstance.serverlessV2('reader', { scaleWithWriter: true })
  ]
});
```

#### Ventajas

1. **Auto-escalado**
   - Escala automáticamente según carga
   - De 0.5 ACU (1 GB RAM) a 2 ACU (4 GB RAM)

2. **Costo Optimizado**
   ```
   Aurora Serverless v2:
   - 0.5 ACU × $0.12/hora = $0.06/hora
   - ~$43/mes en carga baja
   - Escala solo cuando es necesario
   ```

3. **Alta Disponibilidad**
   - Writer + Reader en diferentes AZs
   - Failover automático en < 30 segundos

4. **Backups Automáticos**
   - Snapshots diarios
   - Point-in-time recovery

---

### 3. Computación: ECS Fargate con ALB

**Decisión:** ✅ **ECS Fargate + Application Load Balancer**

```typescript
const cluster = new ecs.Cluster(this, 'FleetCluster', {
  vpc,
  containerInsights: true  // Métricas detalladas
});

const taskDef = new ecs.FargateTaskDefinition(this, 'ServiceTask', {
  cpu: 512,      // 0.5 vCPU
  memoryLimitMiB: 1024  // 1 GB RAM
});

const service = new ecs.FargateService(this, 'Service', {
  cluster,
  taskDefinition: taskDef,
  desiredCount: 2,  // 2 instancias para HA
  assignPublicIp: false,  // Solo en subnet privada
  healthCheckGracePeriod: cdk.Duration.seconds(60)
});

const alb = new elbv2.ApplicationLoadBalancer(this, 'FleetALB', {
  vpc,
  internetFacing: true  // Accesible desde internet
});
```

#### Justificación

1. **Alta Disponibilidad**
   - 2 tareas en diferentes AZs
   - ALB distribuye tráfico
   - Health checks automáticos

2. **Escalabilidad**
   - Auto Scaling basado en CPU/memoria
   - Target Tracking Scaling Policy

3. **Seguridad**
   - Tareas en subnet privada
   - Solo ALB expuesto a internet
   - Security Groups restrictivos

---

### 4. Almacenamiento: S3 con Presigned URLs

**Decisión:** ✅ **S3 Bucket privado + URLs prefirmadas**

```typescript
const documentsBucket = new s3.Bucket(this, 'DocumentsBucket', {
  bucketName: 'fleet-documents',
  encryption: s3.BucketEncryption.S3_MANAGED,
  blockPublicAccess: s3.BlockPublicAccess.BLOCK_ALL,  // Privado
  versioned: true,  // Historial de versiones
  lifecycleRules: [{
    expiration: cdk.Duration.days(90)  // Limpieza automática
  }]
});
```

#### Ventajas de Presigned URLs

1. **Seguridad**
   ```java
   // Backend genera URL temporal (15 min)
   String presignedUrl = s3Client.presignUrl(request);
   
   // Cliente sube directamente a S3
   // Backend nunca maneja el archivo binario
   ```

2. **Escalabilidad**
   - S3 maneja millones de requests/segundo
   - Backend no procesa archivos grandes
   - Reduce carga en ECS

3. **Costo**
   - Backend no paga por transferencia
   - S3 más barato que EC2/Fargate para storage

4. **Rendimiento**
   - Upload directo desde navegador
   - Sin latencia del backend
   - Parallel uploads posibles

---

### 5. Mensajería: SQS

**Decisión:** ✅ **SQS Standard Queue**

```typescript
const queue = new sqs.Queue(this, 'SolicitudesQueue', {
  queueName: 'fleet-solicitudes-queue',
  visibilityTimeout: cdk.Duration.seconds(300),
  retentionPeriod: cdk.Duration.days(14),
  deadLetterQueue: {
    queue: dlq,
    maxReceiveCount: 3  // Después de 3 intentos → DLQ
  }
});
```

#### Justificación

1. **Desacoplamiento**
   - API no espera procesamiento asíncrono
   - Consumidores independientes

2. **Resiliencia**
   - Retry automático
   - Dead Letter Queue para errores
   - Mensajes persistentes

3. **Escalabilidad**
   - Throughput ilimitado
   - Múltiples consumidores

---

## 🔒 Seguridad y Robustez

### 1. Idempotencia y Concurrencia

**Problema:** Cliente hace doble-click → 2 solicitudes duplicadas

**Solución 1: Constraint de Base de Datos**

```sql
ALTER TABLE solicitudes ADD CONSTRAINT uk_patente UNIQUE (patente);
```

```java
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = "patente")
})
public class Solicitud {
    @Column(unique = true, nullable = false)
    private String patente;
}
```

**Solución 2: Bloqueo Optimista**

```java
@Entity
public class Solicitud {
    @Version
    private Long version;  // Hibernate maneja concurrencia
}
```

**Resultado:**
- Primera request: ✅ Crea solicitud
- Segunda request: ❌ `DataIntegrityViolationException`
- Cliente recibe: `409 Conflict - Patente ya registrada`

---

### 2. Sanitización de Inputs

**Implementación:**

```java
@Data
public class CrearSolicitudRequest {
    
    @NotBlank(message = "Nombre del propietario es obligatorio")
    @Size(min = 3, max = 100, message = "Nombre debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", 
             message = "Nombre solo puede contener letras y espacios")
    private String nombrePropietario;
    
    @NotBlank
    @Size(max = 6, message = "Patente no puede exceder 6 caracteres")
    @Pattern(regexp = "^[A-Z]{2,4}\\d{2,4}$", 
             message = "Formato de patente inválido")
    private String patente;
    
    // Sanitización automática
    public void setNombrePropietario(String nombre) {
        this.nombrePropietario = nombre != null ? nombre.trim() : null;
    }
    
    public void setPatente(String patente) {
        this.patente = patente != null ? patente.trim().toUpperCase() : null;
    }
}
```

**Validaciones aplicadas:**
- ✅ Trimming automático
- ✅ Longitudes máximas
- ✅ Expresiones regulares
- ✅ Normalización (uppercase para patentes)

---

### 3. Manejo de Errores Sin Stack Traces

**GlobalExceptionHandler:**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        // NO expone stack trace
        return ResponseEntity.badRequest().body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Validation Error")
                .message("Error de validación en los datos enviados")
                .errors(errors)
                .build()
        );
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(Exception ex) {
        log.error("Error interno: ", ex);  // Log completo en servidor
        
        // Cliente solo recibe mensaje genérico
        return ResponseEntity.status(500).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(500)
                .error("Internal Server Error")
                .message("Ha ocurrido un error interno. Contacte al administrador.")
                .build()
        );
    }
}
```

**Configuración en application.yaml:**

```yaml
server:
  error:
    include-message: always
    include-binding-errors: always
    include-stacktrace: never  # ← Crítico
    include-exception: false   # ← Crítico
```

---

### 4. Principio de Mínimo Privilegio (IAM)

**Task Role (ECS):**

```typescript
const taskRole = new iam.Role(this, 'TaskRole', {
  assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
});

// Solo lectura/escritura en bucket específico
documentsBucket.grantReadWrite(taskRole);

// Solo envío de mensajes a cola específica
queue.grantSendMessages(taskRole);

// Solo lectura de secreto específico
dbCredentials.grantRead(taskRole);

// NO tiene permisos para:
// - Eliminar buckets
// - Modificar IAM
// - Acceder a otros recursos
```

**Security Groups:**

```typescript
// ECS solo acepta tráfico del ALB
service.connections.allowFrom(
  alb,
  ec2.Port.tcp(8080),
  'Allow traffic from ALB'
);

// RDS solo acepta tráfico de ECS
db.connections.allowFrom(
  service,
  ec2.Port.tcp(5432),
  'Allow traffic from ECS'
);
```

---

## 📊 Justificaciones Técnicas

### ¿Por qué Spring Boot 3.5 con Java 21?

1. **Virtual Threads (Project Loom)**
   ```java
   @EnableAsync
   @Configuration
   public class AsyncConfig {
       @Bean
       public Executor taskExecutor() {
           return Executors.newVirtualThreadPerTaskExecutor();
       }
   }
   ```
   - Manejo eficiente de I/O
   - Miles de threads concurrentes
   - Menor consumo de memoria

2. **Record Patterns**
   ```java
   public record SolicitudCreadaEvent(
       UUID solicitudId,
       String patente,
       String propietario
   ) {}
   ```

3. **Pattern Matching**
   ```java
   if (exception instanceof DataIntegrityViolationException dive) {
       return handleDuplicateKey(dive);
   }
   ```

---

### ¿Por qué Aurora Serverless v2 y no RDS tradicional?

| Aspecto | RDS Tradicional | Aurora Serverless v2 |
|---------|----------------|---------------------|
| **Costo mínimo** | ~$50/mes (t3.micro) | ~$43/mes (0.5 ACU) |
| **Escalado** | Manual | Automático |
| **Downtime** | Sí (resize) | No |
| **HA** | Opcional | Incluido |
| **Backups** | Manual config | Automático |

---

### ¿Por qué ECS Fargate y no EC2?

| Aspecto | EC2 | ECS Fargate |
|---------|-----|-------------|
| **Gestión OS** | Manual | Automática |
| **Patching** | Manual | Automático |
| **Escalado** | Complejo | Automático |
| **Costo** | Fijo | Por uso |
| **Overhead** | Alto | Bajo |

---

## 🎯 Conclusión

Las decisiones arquitectónicas tomadas priorizan:

1. ✅ **Robustez**: Validaciones múltiples, manejo de errores, idempotencia
2. ✅ **Seguridad**: Mínimo privilegio, sanitización, sin stack traces
3. ✅ **Escalabilidad**: Auto-scaling, arquitectura desacoplada
4. ✅ **Mantenibilidad**: Clean architecture, código limpio, tests
5. ✅ **Costo-efectividad**: Serverless donde aplica, sizing adecuado

El resultado es un sistema **production-ready** que cumple todos los requerimientos de la prueba técnica y está preparado para escalar.

---

**Última actualización:** 2026-02-16  
**Versión:** 1.0.0
