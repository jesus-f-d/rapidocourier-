import { useEffect, useState } from 'react';
import { api } from '../api/client';

// Los GET de paquetes no incluyen los nombres (el backend solo los enriquece al crear),
// así que se resuelven aquí consultando servicio-clientes y cacheando por id.
const cache = new Map();

export function useNombresClientes(token, paquetes) {
  const [nombres, setNombres] = useState({});

  useEffect(() => {
    if (!paquetes || paquetes.length === 0) return;

    const pendientes = new Set();
    for (const p of paquetes) {
      if (!p.remitenteNombre && p.remitenteId && !cache.has(p.remitenteId)) pendientes.add(p.remitenteId);
      if (!p.destinatarioNombre && p.destinatarioId && !cache.has(p.destinatarioId)) pendientes.add(p.destinatarioId);
    }

    let activo = true;
    Promise.all(
      [...pendientes].map((id) =>
        api.obtenerCliente(token, id)
          .then((r) => cache.set(id, r.data.nombreCompleto))
          .catch(() => cache.set(id, null)),
      ),
    ).then(() => {
      if (!activo) return;
      const mapa = {};
      for (const p of paquetes) {
        if (cache.has(p.remitenteId)) mapa[p.remitenteId] = cache.get(p.remitenteId);
        if (cache.has(p.destinatarioId)) mapa[p.destinatarioId] = cache.get(p.destinatarioId);
      }
      setNombres(mapa);
    });

    return () => { activo = false; };
  }, [token, paquetes]);

  return nombres;
}
