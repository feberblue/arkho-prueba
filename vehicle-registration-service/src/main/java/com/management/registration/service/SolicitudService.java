package com.management.registration.service;

import com.management.registration.dto.request.CrearSolicitudRequest;
import com.management.registration.dto.response.SolicitudResponse;
import com.management.registration.entity.Solicitud;
import com.management.registration.exception.PatenteYaRegistradaException;
import com.management.registration.exception.SolicitudNotFoundException;
import com.management.registration.repository.SolicitudRepository;
import com.management.registration.entity.EstadoSolicitud;
import com.management.registration.validator.PatenteValidator;
import com.management.registration.validator.RutValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudService {

    private final SolicitudRepository solicitudRepository;

    /**
     * Crea una nueva solicitud de inscripción
     */
    @Transactional
    public SolicitudResponse crearSolicitud(CrearSolicitudRequest request) {
        log.info("Iniciando creación de solicitud para patente: {}", request.getPatente());

        // 1. Sanitización de datos
        String patenteLimpia = sanitizarPatente(request.getPatente());
        String rutLimpio = sanitizarRut(request.getRut());

        // 2. Validaciones de negocio adicionales
        validarAnioVehiculo(request.getAnio());

        // 3. Verificar duplicados antes de intentar guardar
        if (solicitudRepository.existsByPatente(patenteLimpia)) {
            log.warn("Intento de registrar patente duplicada: {}", patenteLimpia);
            throw new PatenteYaRegistradaException(patenteLimpia);
        }

        // 4. Crear entidad
        Solicitud solicitud = mapearAEntidad(request, patenteLimpia, rutLimpio);

        // 5. Guardar en base de datos
        try {
            Solicitud solicitudGuardada = solicitudRepository.save(solicitud);
            log.info("Solicitud creada exitosamente con ID: {}", solicitudGuardada.getId());
            return mapearARespuesta(solicitudGuardada);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al guardar solicitud: {}", e.getMessage());
            throw new PatenteYaRegistradaException(patenteLimpia);
        }
    }

    /**
     * Obtiene todas las solicitudes con paginación
     */
    @Transactional()
    public Page<SolicitudResponse> obtenerSolicitudes(Pageable pageable) {
        log.debug("Obteniendo solicitudes - Página: {}, Tamaño: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Solicitud> solicitudes = solicitudRepository.findAll(pageable);
        return solicitudes.map(this::mapearARespuesta);
    }

    /**
     * Obtiene una solicitud por su ID
     */
    @Transactional()
    public SolicitudResponse obtenerSolicitudPorId(UUID id) {
        log.debug("Buscando solicitud con ID: {}", id);

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new SolicitudNotFoundException(id));

        return mapearARespuesta(solicitud);
    }

    // ========== Métodos de Sanitización ==========

    private String sanitizarPatente(String patente) {
        if (patente == null) {
            return null;
        }
        return PatenteValidator.normalizarPatente(patente);
    }

    private String sanitizarRut(String rut) {
        if (rut == null) {
            return null;
        }
        return RutValidator.normalizarRut(rut);
    }

    private String sanitizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.trim();
    }

    // ========== Validaciones de Negocio ==========

    private void validarAnioVehiculo(Integer anio) {
        int anioActual = Year.now().getValue();
        if (anio > anioActual) {
            throw new IllegalArgumentException(
                    String.format("El año del vehículo (%d) no puede ser futuro. Año actual: %d",
                            anio, anioActual));
        }
    }

    // ========== Mapeo de Objetos ==========

    private Solicitud mapearAEntidad(CrearSolicitudRequest request, String patenteLimpia, String rutLimpio) {
        return Solicitud.builder()
                .nombrePropietario(sanitizarTexto(request.getNombrePropietario()))
                .rut(rutLimpio)
                .email(sanitizarTexto(request.getEmail()))
                .telefono(sanitizarTexto(request.getTelefono()))
                .patente(patenteLimpia)
                .marca(sanitizarTexto(request.getMarca()))
                .modelo(sanitizarTexto(request.getModelo()))
                .anio(request.getAnio())
                .color(sanitizarTexto(request.getColor()))
                .tipoVehiculo(sanitizarTexto(request.getTipoVehiculo()))
                .observaciones(sanitizarTexto(request.getObservaciones()))
                .estado(EstadoSolicitud.PENDIENTE)
                .build();
    }

    private SolicitudResponse mapearARespuesta(Solicitud solicitud) {
        return SolicitudResponse.builder()
                .id(solicitud.getId())
                .nombrePropietario(solicitud.getNombrePropietario())
                .rut(solicitud.getRut())
                .email(solicitud.getEmail())
                .telefono(solicitud.getTelefono())
                .patente(solicitud.getPatente())
                .marca(solicitud.getMarca())
                .modelo(solicitud.getModelo())
                .anio(solicitud.getAnio())
                .color(solicitud.getColor())
                .tipoVehiculo(solicitud.getTipoVehiculo())
                .estado(solicitud.getEstado())
                .observaciones(solicitud.getObservaciones())
                .fechaCreacion(solicitud.getFechaCreacion())
                .fechaActualizacion(solicitud.getFechaActualizacion())
                .build();
    }
}
