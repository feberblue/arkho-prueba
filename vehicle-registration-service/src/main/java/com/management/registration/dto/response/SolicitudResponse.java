package com.management.registration.dto.response;

import com.management.registration.entity.EstadoSolicitud;
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
public class SolicitudResponse {

    private UUID id;
    private String nombrePropietario;
    private String rut;
    private String email;
    private String telefono;
    private String patente;
    private String marca;
    private String modelo;
    private Integer anio;
    private String color;
    private String tipoVehiculo;
    private EstadoSolicitud estado;
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
