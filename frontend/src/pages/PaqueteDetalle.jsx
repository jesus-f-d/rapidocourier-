import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';
import EstadoBadge from '../components/EstadoBadge.jsx';
import Modal from '../components/Modal.jsx';
import { useNombresClientes } from '../hooks/useNombresClientes.js';

// Transiciones válidas según RF-04
const TRANSICIONES = {
  REGISTRADO: ['EN_ALMACEN', 'CANCELADO'],
  EN_ALMACEN: ['EN_TRANSITO', 'CANCELADO'],
  EN_TRANSITO: ['EN_DESTINO', 'CANCELADO'],
  EN_DESTINO: ['ENTREGADO'],
  ENTREGADO: [],
  CANCELADO: [],
};

export default function PaqueteDetalle() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { token, puedeGestionar, esAdmin } = useAuth();
  const [paquete, setPaquete] = useState(null);
  const [historial, setHistorial] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(null);
  const [modalEstado, setModalEstado] = useState(false);
  const [nuevoEstado, setNuevoEstado] = useState('');
  const [observacion, setObservacion] = useState('');
  const [categoriaSel, setCategoriaSel] = useState('');
  const [procesando, setProcesando] = useState(false);
  const [listaPaquete, setListaPaquete] = useState([]);
  const nombres = useNombresClientes(token, listaPaquete);

  const cargar = useCallback(() => {
    Promise.all([
      api.obtenerPaquete(token, id),
      api.obtenerHistorial(token, id),
    ])
      .then(([p, h]) => {
        setPaquete(p.data);
        setListaPaquete([p.data]);
        setHistorial(h.data || []);
      })
      .catch((err) => setError(err.message));
  }, [token, id]);

  useEffect(() => { cargar(); }, [cargar]);

  useEffect(() => {
    if (puedeGestionar) {
      api.listarCategorias(token)
        .then((res) => setCategorias(res.data || []))
        .catch(() => {});
    }
  }, [token, puedeGestionar]);

  const cambiarEstado = async (e) => {
    e.preventDefault();
    setProcesando(true);
    setError(null);
    setOk(null);
    try {
      await api.cambiarEstado(token, id, nuevoEstado, observacion.trim() || undefined);
      setOk(`Estado actualizado a ${nuevoEstado.replace('_', ' ')}.`);
      setModalEstado(false);
      setNuevoEstado('');
      setObservacion('');
      cargar();
    } catch (err) {
      setError(err.message);
      setModalEstado(false);
    } finally {
      setProcesando(false);
    }
  };

  const asignarCategoria = async () => {
    if (!categoriaSel) return;
    setError(null);
    setOk(null);
    try {
      await api.asignarCategoria(token, id, categoriaSel);
      setOk('Categoría asignada al paquete.');
      setCategoriaSel('');
      cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  const eliminar = async () => {
    if (!window.confirm(`¿Eliminar el paquete ${paquete.codigoRastreo}? Esta acción no se puede deshacer.`)) return;
    try {
      await api.eliminarPaquete(token, id);
      navigate('/paquetes');
    } catch (err) {
      setError(err.message);
    }
  };

  if (!paquete) {
    return (
      <>
        {error ? <div className="alerta error">{error}</div> : <div className="vacio">Cargando…</div>}
        <Link to="/paquetes" className="btn btn-borde btn-sm">← Volver a paquetes</Link>
      </>
    );
  }

  const transicionesDisponibles = TRANSICIONES[paquete.estado] || [];
  const fmt = (f) => (f ? new Date(f).toLocaleString('es-PE') : '—');

  return (
    <>
      <div className="cabecera-pagina">
        <div>
          <h1>{paquete.codigoRastreo}</h1>
          <p>Registrado el {fmt(paquete.createdAt)}</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Link to="/paquetes" className="btn btn-borde">← Volver</Link>
          {puedeGestionar && transicionesDisponibles.length > 0 && (
            <button className="btn btn-primario" onClick={() => { setNuevoEstado(transicionesDisponibles[0]); setModalEstado(true); }}>
              Cambiar estado
            </button>
          )}
          {esAdmin && (
            <button className="btn btn-peligro" onClick={eliminar}>Eliminar</button>
          )}
        </div>
      </div>

      {error && <div className="alerta error">{error}</div>}
      {ok && <div className="alerta ok">{ok}</div>}

      <div className="tarjeta">
        <div className="grid-detalle">
          <div className="dato">
            <div className="etiqueta">Estado actual</div>
            <div className="valor"><EstadoBadge estado={paquete.estado} /></div>
          </div>
          <div className="dato">
            <div className="etiqueta">Remitente</div>
            <div className="valor">{paquete.remitenteNombre || nombres[paquete.remitenteId] || paquete.remitenteId}</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Destinatario</div>
            <div className="valor">{paquete.destinatarioNombre || nombres[paquete.destinatarioId] || paquete.destinatarioId}</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Ruta</div>
            <div className="valor">{paquete.sucursalOrigen} → {paquete.sucursalDestino}</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Peso</div>
            <div className="valor">{Number(paquete.pesoKg).toFixed(2)} kg</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Distancia</div>
            <div className="valor">{Number(paquete.distanciaKm).toFixed(1)} km</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Valor declarado</div>
            <div className="valor">S/ {Number(paquete.valorDeclarado).toFixed(2)}</div>
          </div>
          <div className="dato">
            <div className="etiqueta">Tarifa calculada</div>
            <div className="valor" style={{ color: 'var(--naranja-oscuro)' }}>
              S/ {Number(paquete.tarifa).toFixed(2)}
            </div>
          </div>
        </div>
        {paquete.descripcion && (
          <p style={{ marginBottom: 0, color: 'var(--texto-suave)' }}>
            <strong>Descripción:</strong> {paquete.descripcion}
          </p>
        )}
      </div>

      {puedeGestionar && (
        <div className="tarjeta">
          <h2 style={{ fontSize: 17, marginBottom: 12 }}>Categorías</h2>
          <div className="chips" style={{ marginBottom: 14 }}>
            {(paquete.categorias || []).length === 0
              ? <span className="ayuda">Sin categorías asignadas.</span>
              : [...paquete.categorias].map((c) => <span key={c} className="chip">{c}</span>)}
          </div>
          <div className="barra-filtros" style={{ marginBottom: 0 }}>
            <select value={categoriaSel} onChange={(e) => setCategoriaSel(e.target.value)}>
              <option value="">Seleccionar categoría…</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
            <button className="btn btn-secundario btn-sm" onClick={asignarCategoria} disabled={!categoriaSel}>
              Asignar
            </button>
          </div>
        </div>
      )}

      <div className="tarjeta">
        <h2 style={{ fontSize: 17, marginBottom: 16 }}>Historial de estados</h2>
        {historial.length === 0 ? (
          <div className="vacio">Sin cambios de estado registrados.</div>
        ) : (
          <ul className="timeline">
            {historial.map((h) => (
              <li key={h.id}>
                <div className="fecha">{fmt(h.fechaCambio)} — {h.usuarioEmail}</div>
                <div className="detalle">
                  {h.estadoAnterior
                    ? <><EstadoBadge estado={h.estadoAnterior} /> → <EstadoBadge estado={h.estadoNuevo} /></>
                    : <EstadoBadge estado={h.estadoNuevo} />}
                  {h.observacion && <div className="ayuda">“{h.observacion}”</div>}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {modalEstado && (
        <Modal titulo="Cambiar estado del paquete" onCerrar={() => setModalEstado(false)}>
          <form onSubmit={cambiarEstado}>
            <div className="campo">
              <label>Estado actual</label>
              <EstadoBadge estado={paquete.estado} />
            </div>
            <div className="campo">
              <label>Nuevo estado</label>
              <select value={nuevoEstado} onChange={(e) => setNuevoEstado(e.target.value)} required>
                {transicionesDisponibles.map((t) => (
                  <option key={t} value={t}>{t.replace('_', ' ')}</option>
                ))}
              </select>
              <div className="ayuda">Solo se muestran las transiciones permitidas (RF-04).</div>
            </div>
            <div className="campo">
              <label>Observación (opcional)</label>
              <textarea
                rows={2}
                maxLength={300}
                value={observacion}
                onChange={(e) => setObservacion(e.target.value)}
                placeholder="Ej. Recibido en almacén central"
              />
            </div>
            <div className="acciones">
              <button type="button" className="btn btn-borde" onClick={() => setModalEstado(false)}>Cancelar</button>
              <button className="btn btn-primario" disabled={procesando}>
                {procesando ? 'Actualizando…' : 'Actualizar estado'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
