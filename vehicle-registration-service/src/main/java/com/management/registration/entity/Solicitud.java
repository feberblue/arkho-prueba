package com.management.registration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="solicitudes",
    uniqueConstraints = {
        @UniqueConstraint(name="uk_patente", columnNames = "patente")
})
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nombre_propietario", nullable = false, length = 200)
    private String nombrePropietario;

    @Column(name = "rut", nullable = false, length = 12)
    private String rut;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "telefono", length = 20)
    private String telefono;

    // Datos del Vehículo
    @Column(name = "patente", nullable = false, unique = true, length = 10)
    private String patente;

    @Column(name = "marca", nullable = false, length = 50)
    private String marca;

    @Column(name = "modelo", nullable = false, length = 50)
    private String modelo;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "tipo_vehiculo", length = 50)
    private String tipoVehiculo;

    // Estado y Control
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    // Optimistic Locking
    @Version
    @Column(name = "version")
    private Long version;

    // Auditoría
    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        if (estado == null) {
            estado = EstadoSolicitud.PENDIENTE;
        }
    }
}
