package com.management.registration.service;

import com.management.registration.dto.request.CrearSolicitudRequest;
import com.management.registration.dto.response.SolicitudResponse;
import com.management.registration.entity.EstadoSolicitud;
import com.management.registration.entity.Solicitud;
import com.management.registration.event.EventPublisher;
import com.management.registration.exception.PatenteYaRegistradaException;
import com.management.registration.exception.SolicitudNotFoundException;
import com.management.registration.repository.SolicitudRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SolicitudServiceTest {

    @Mock
    private SolicitudRepository solicitudRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SolicitudService solicitudService;

    private CrearSolicitudRequest requestValido;
    private Solicitud solicitudMock;

    @BeforeEach
    void setUp() {
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
                .observaciones("Ninguna")
                .build();

        solicitudMock = Solicitud.builder()
                .id(UUID.randomUUID())
                .nombrePropietario("Juan Pérez")
                .rut("123456789")
                .email("juan.perez@example.com")
                .patente("ABCD12")
                .marca("Toyota")
                .modelo("Corolla")
                .anio(2023)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe crear solicitud exitosamente con datos válidos")
    void crearSolicitud_ConDatosValidos_DebeCrearExitosamente() {
        // Given
        when(solicitudRepository.existsByPatente(anyString())).thenReturn(false);
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(solicitudMock);
        doNothing().when(eventPublisher).publicarSolicitudCreada(any(Solicitud.class));

        // When
        SolicitudResponse response = solicitudService.crearSolicitud(requestValido);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("ABCD12", response.getPatente());
        assertEquals(EstadoSolicitud.PENDIENTE, response.getEstado());

        verify(solicitudRepository).existsByPatente("ABCD12");
        verify(solicitudRepository).save(any(Solicitud.class));
        verify(eventPublisher).publicarSolicitudCreada(any(Solicitud.class));
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la patente ya existe")
    void crearSolicitud_PatenteDuplicada_DebeLanzarExcepcion() {
        // Given
        when(solicitudRepository.existsByPatente(anyString())).thenReturn(true);

        // When & Then
        assertThrows(PatenteYaRegistradaException.class, () ->
                solicitudService.crearSolicitud(requestValido)
        );

        verify(solicitudRepository, never()).save(any(Solicitud.class));
        verify(eventPublisher, never()).publicarSolicitudCreada(any(Solicitud.class));
    }

    @Test
    @DisplayName("Debe manejar race condition en patente duplicada")
    void crearSolicitud_RaceCondition_DebeLanzarExcepcion() {
        // Given
        when(solicitudRepository.existsByPatente(anyString())).thenReturn(false);
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When & Then
        assertThrows(PatenteYaRegistradaException.class, () ->
                solicitudService.crearSolicitud(requestValido)
        );

        verify(eventPublisher, never()).publicarSolicitudCreada(any(Solicitud.class));
    }

    @Test
    @DisplayName("Debe validar que el año no sea futuro")
    void crearSolicitud_AnioFuturo_DebeLanzarExcepcion() {
        // Given
        requestValido.setAnio(2030);

        // When & Then
        assertThrows(IllegalArgumentException.class, () ->
                solicitudService.crearSolicitud(requestValido)
        );

        verify(solicitudRepository, never()).save(any(Solicitud.class));
    }

    @Test
    @DisplayName("Debe normalizar la patente correctamente")
    void crearSolicitud_PatenteConEspacios_DebeNormalizar() {
        // Given
        requestValido.setPatente("  ab-cd12  ");
        when(solicitudRepository.existsByPatente("ABCD12")).thenReturn(false);
        when(solicitudRepository.save(any(Solicitud.class))).thenReturn(solicitudMock);

        // When
        solicitudService.crearSolicitud(requestValido);

        // Then
        ArgumentCaptor<Solicitud> captor = ArgumentCaptor.forClass(Solicitud.class);
        verify(solicitudRepository).save(captor.capture());
        assertEquals("ABCD12", captor.getValue().getPatente());
    }

    @Test
    @DisplayName("Debe obtener solicitudes con paginación")
    void obtenerSolicitudes_ConPaginacion_DebeRetornarPagina() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Solicitud> page = new PageImpl<>(List.of(solicitudMock));
        when(solicitudRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<SolicitudResponse> result = solicitudService.obtenerSolicitudes(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(solicitudRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Debe obtener solicitud por ID exitosamente")
    void obtenerSolicitudPorId_IdValido_DebeRetornarSolicitud() {
        // Given
        UUID id = solicitudMock.getId();
        when(solicitudRepository.findById(id)).thenReturn(Optional.of(solicitudMock));

        // When
        SolicitudResponse response = solicitudService.obtenerSolicitudPorId(id);

        // Then
        assertNotNull(response);
        assertEquals(id, response.getId());
        verify(solicitudRepository).findById(id);
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando solicitud no existe")
    void obtenerSolicitudPorId_IdInvalido_DebeLanzarExcepcion() {
        // Given
        UUID id = UUID.randomUUID();
        when(solicitudRepository.findById(id)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(SolicitudNotFoundException.class, () ->
                solicitudService.obtenerSolicitudPorId(id)
        );
    }
}
