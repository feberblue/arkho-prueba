package com.management.registration.controller;

import com.management.registration.dto.request.CrearSolicitudRequest;
import com.management.registration.dto.response.SolicitudResponse;
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
}
