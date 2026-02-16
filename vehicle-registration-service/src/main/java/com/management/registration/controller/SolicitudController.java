package com.management.registration.controller;

import com.management.registration.dto.request.CrearSolicitudRequest;
import com.management.registration.dto.response.PresignedUrlResponse;
import com.management.registration.dto.response.SolicitudResponse;
import com.management.registration.service.PresignedUrlService;
import com.management.registration.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/solicitudes")
@RequiredArgsConstructor
@Slf4j
public class SolicitudController {

    private final SolicitudService solicitudService;
    private final PresignedUrlService presignedUrlService;

    // Endpoint Health para ssaber salud de servicio
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @PostMapping
    public ResponseEntity<SolicitudResponse> crearSolicitud(
            @Valid @RequestBody CrearSolicitudRequest request) {

        log.info("Recibida solicitud de creación para patente: {}", request.getPatente());
        SolicitudResponse response = solicitudService.crearSolicitud(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SolicitudResponse>> obtenerSolicitudes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.debug("Obteniendo solicitudes - page: {}, size: {}, sort: {} {}",
                page, size, sortBy, sortDir);

        // Validar límites de paginación
        if (size > 100) {
            size = 100; // Máximo 100 elementos por página
        }

        Sort sort = sortDir.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() :
                Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SolicitudResponse> solicitudes = solicitudService.obtenerSolicitudes(pageable);
        return ResponseEntity.ok(solicitudes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudResponse> obtenerSolicitudPorId(
            @PathVariable UUID id) {

        log.debug("Obteniendo solicitud con ID: {}", id);
        SolicitudResponse response = solicitudService.obtenerSolicitudPorId(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/documentos/upload-url")
    public ResponseEntity<PresignedUrlResponse> generarUrlDeSubida(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "documento") String tipoDocumento) {

        log.info("Generando URL de subida para solicitud: {}, tipo: {}", id, tipoDocumento);

        // Verificar que la solicitud existe
        solicitudService.obtenerSolicitudPorId(id);

        // Generar URL prefirmada
        PresignedUrlResponse response = presignedUrlService.generarUrlParaSubida(id, tipoDocumento);
        return ResponseEntity.ok(response);
    }
}
