package com.management.registration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.registration.dto.request.CrearSolicitudRequest;
import com.management.registration.repository.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SolicitudControllerIntegrationTest {

        @Container
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");

        @DynamicPropertySource
        static void configureProperties(DynamicPropertyRegistry registry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("aws.s3.enabled", () -> "false");
            registry.add("aws.sqs.enabled", () -> "false");
        }

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private SolicitudRepository solicitudRepository;

        private CrearSolicitudRequest requestValido;

        @BeforeEach
        void setUp() {
            solicitudRepository.deleteAll();

            requestValido = CrearSolicitudRequest.builder()
                    .nombrePropietario("Juan Pérez")
                    .rut("12345678-9")
                    .email("juan.perez@example.com")
                    .telefono("+56912345678")
                    .patente("ABCD12")
                    .marca("Toyota")
                    .modelo("Corolla")
                    .anio(2023)
                    .color("Blanco")
                    .tipoVehiculo("Sedan")
                    .build();
        }

        @Test
        @DisplayName("POST /api/v1/solicitudes - Debe crear solicitud exitosamente")
        void crearSolicitud_ConDatosValidos_DebeRetornar201() throws Exception {
            mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.patente").value("ABCD12"))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                    .andExpect(jsonPath("$.nombrePropietario").value("Juan Pérez"));
        }

        @Test
        @DisplayName("POST /api/v1/solicitudes - Debe retornar 400 con datos inválidos")
        void crearSolicitud_ConDatosInvalidos_DebeRetornar400() throws Exception {
            requestValido.setEmail("email-invalido");
            requestValido.setPatente("INVALIDA");

            mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("POST /api/v1/solicitudes - Debe retornar 409 con patente duplicada")
        void crearSolicitud_PatenteDuplicada_DebeRetornar409() throws Exception {
            // Primera creación
            mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isCreated());

            // Intento de duplicar
            mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value(containsString("ya se encuentra registrada")));
        }

        @Test
        @DisplayName("GET /api/v1/solicitudes - Debe obtener solicitudes con paginación")
        void obtenerSolicitudes_DebeRetornarPaginado() throws Exception {
            // Crear algunas solicitudes
            for (int i = 1; i <= 3; i++) {
                CrearSolicitudRequest request = CrearSolicitudRequest.builder()
                        .nombrePropietario("Propietario " + i)
                        .rut("1234567" + i + "-9")
                        .email("email" + i + "@example.com")
                        .patente("ABCD" + (10 + i))
                        .marca("Marca" + i)
                        .modelo("Modelo" + i)
                        .anio(2023)
                        .build();

                mockMvc.perform(post("/api/v1/solicitudes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
            }

            // Obtener con paginación
            mockMvc.perform(get("/api/v1/solicitudes")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("GET /api/v1/solicitudes/{id} - Debe obtener solicitud por ID")
        void obtenerSolicitudPorId_IdValido_DebeRetornar200() throws Exception {
            // Crear solicitud
            String response = mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String id = objectMapper.readTree(response).get("id").asText();

            // Obtener por ID
            mockMvc.perform(get("/api/v1/solicitudes/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.patente").value("ABCD12"));
        }

        @Test
        @DisplayName("GET /api/v1/solicitudes/{id} - Debe retornar 404 con ID inexistente")
        void obtenerSolicitudPorId_IdInexistente_DebeRetornar404() throws Exception {
            String idInexistente = "123e4567-e89b-12d3-a456-426614174000";

            mockMvc.perform(get("/api/v1/solicitudes/" + idInexistente))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("POST /api/v1/solicitudes/{id}/documentos/upload-url - Debe generar URL")
        void generarUrlDeSubida_SolicitudExistente_DebeRetornar200() throws Exception {
            // Crear solicitud
            String response = mockMvc.perform(post("/api/v1/solicitudes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestValido)))
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String id = objectMapper.readTree(response).get("id").asText();

            // Generar URL
            mockMvc.perform(post("/api/v1/solicitudes/" + id + "/documentos/upload-url")
                            .param("tipoDocumento", "padron"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.uploadUrl").exists())
                    .andExpect(jsonPath("$.fileKey").exists())
                    .andExpect(jsonPath("$.expiresAt").exists());
        }
    }
