package com.rapidocourier.paquetes.entity;

import com.rapidocourier.paquetes.enums.EstadoPaquete;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "historial_estados")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paquete_id", nullable = false)
    private Paquete paquete;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private EstadoPaquete estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPaquete estadoNuevo;

    @Column(nullable = false, length = 100)
    private String usuarioEmail;

    @Column(length = 300)
    private String observacion;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime fechaCambio;
}
