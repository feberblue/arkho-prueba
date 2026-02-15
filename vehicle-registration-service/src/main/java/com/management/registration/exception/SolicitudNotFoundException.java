package com.management.registration.exception;

import java.util.UUID;

public class SolicitudNotFoundException extends RuntimeException {

    private final UUID solicitudId;

    public SolicitudNotFoundException(UUID solicitudId) {
        super(String.format("Solicitud con ID '%s' no encontrada", solicitudId));
        this.solicitudId = solicitudId;
    }

    public UUID getSolicitudId() {
        return solicitudId;
    }
}
