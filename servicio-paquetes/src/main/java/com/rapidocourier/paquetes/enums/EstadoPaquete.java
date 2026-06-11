package com.rapidocourier.paquetes.enums;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoPaquete {
    REGISTRADO,
    EN_ALMACEN,
    EN_TRANSITO,
    EN_DESTINO,
    ENTREGADO,
    CANCELADO;

    public Set<EstadoPaquete> transicionesPermitidas() {
        return switch (this) {
            case REGISTRADO  -> EnumSet.of(EN_ALMACEN, CANCELADO);
            case EN_ALMACEN  -> EnumSet.of(EN_TRANSITO, CANCELADO);
            case EN_TRANSITO -> EnumSet.of(EN_DESTINO, CANCELADO);
            case EN_DESTINO  -> EnumSet.of(ENTREGADO);
            case ENTREGADO, CANCELADO -> EnumSet.noneOf(EstadoPaquete.class);
        };
    }

    public boolean puedeTransicionarA(EstadoPaquete siguiente) {
        return transicionesPermitidas().contains(siguiente);
    }
}
