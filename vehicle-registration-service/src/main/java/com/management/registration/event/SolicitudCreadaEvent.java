package com.management.registration.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudCreadaEvent {
    private UUID solicitudId;
    private String patente;
    private String nombrePropietario;
    private String rut;
    private String email;
    private LocalDateTime fechaCreacion;
    private String eventType;

    public static SolicitudCreadaEvent fromSolicitud(
            UUID solicitudId,
            String patente,
            String nombrePropietario,
            String rut,
            String email) {

        return SolicitudCreadaEvent.builder()
                .solicitudId(solicitudId)
                .patente(patente)
                .nombrePropietario(nombrePropietario)
                .rut(rut)
                .email(email)
                .fechaCreacion(LocalDateTime.now())
                .eventType("SOLICITUD_CREADA")
                .build();
    }
}
