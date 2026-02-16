package com.management.registration.dto.request;

import com.management.registration.validator.ValidPatente;
import com.management.registration.validator.ValidRut;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearSolicitudRequest {
    // Datos del Propietario
    @NotBlank(message = "El nombre del propietario es obligatorio")
    @Size(min = 3, max = 200, message = "El nombre debe tener entre 3 y 200 caracteres")
    private String nombrePropietario;

    @NotBlank(message = "El RUT es obligatorio")
    @ValidRut
    private String rut;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 100, message = "El email no puede exceder 100 caracteres")
    private String email;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^[+]?[0-9]{8,20}$", message = "El teléfono debe ser válido")
    private String telefono;

    // Datos del Vehículo
    @NotBlank(message = "La patente es obligatoria")
    @ValidPatente
    private String patente;

    @NotBlank(message = "La marca es obligatoria")
    @Size(min = 2, max = 50, message = "La marca debe tener entre 2 y 50 caracteres")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(min = 1, max = 50, message = "El modelo debe tener entre 1 y 50 caracteres")
    private String modelo;

    @NotNull(message = "El año es obligatorio")
    @Min(value = 1900, message = "El año debe ser mayor a 1900")
    @Max(value = 2027, message = "El año no puede ser futuro")
    private Integer anio;

    @Size(max = 30, message = "El color no puede exceder 30 caracteres")
    private String color;

    @Size(max = 50, message = "El tipo de vehículo no puede exceder 50 caracteres")
    private String tipoVehiculo;

    @Size(max = 500, message = "Las observaciones no pueden exceder 500 caracteres")
    private String observaciones;

    public void setNombrePropietario(String nombrePropietario) {
        this.nombrePropietario = nombrePropietario != null ? nombrePropietario.trim() : null;
    }

    public void setPatente(String patente) {
        this.patente = patente != null ? patente.trim().toUpperCase() : null;
    }

    public void setMarca(String marca) {
        this.marca = marca != null ? marca.trim() : null;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo != null ? modelo.trim() : null;
    }

    public void setColor(String color) {
        this.color = color != null ? color.trim() : null;
    }

    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo != null ? tipoVehiculo.trim() : null;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones != null ? observaciones.trim() : null;
    }
}
