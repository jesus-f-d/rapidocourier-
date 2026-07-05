package com.rapidocourier.paquetes.entity;

import com.rapidocourier.paquetes.enums.EstadoPaquete;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "paquetes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Paquete {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigoRastreo;

    @Column(nullable = false)
    private UUID remitenteId;

    @Column(nullable = false)
    private UUID destinatarioId;

    @Column(nullable = false, length = 100)
    private String sucursalOrigen;

    @Column(nullable = false, length = 100)
    private String sucursalDestino;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal pesoKg;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDeclarado;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal distanciaKm;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifa;

    @Column(length = 500)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoPaquete estado = EstadoPaquete.REGISTRADO;

    // Excluidas de equals/hashCode/toString para evitar recursion infinita
    // entre Paquete y Categoria (relacion bidireccional M:M)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "paquete", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HistorialEstado> historial = new ArrayList<>();

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "paquete_categorias",
            joinColumns = @JoinColumn(name = "paquete_id"),
            inverseJoinColumns = @JoinColumn(name = "categoria_id")
    )
    @Builder.Default
    private Set<Categoria> categorias = new HashSet<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
