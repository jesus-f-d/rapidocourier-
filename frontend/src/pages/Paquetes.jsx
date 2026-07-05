import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';
import EstadoBadge from '../components/EstadoBadge.jsx';
import Modal from '../components/Modal.jsx';
import { useNombresClientes } from '../hooks/useNombresClientes.js';

const ESTADOS = ['REGISTRADO', 'EN_ALMACEN', 'EN_TRANSITO', 'EN_DESTINO', 'ENTREGADO', 'CANCELADO'];
const FORM_VACIO = {
  remitenteId: '',
  destinatarioId: '',
  sucursalOrigen: '',
  sucursalDestino: '',
  pesoKg: '',
  valorDeclarado: '',
  distanciaKm: '',
  descripcion: '',
};

// Fórmula RF-03 (los valores reales los aplica el backend desde el Config Server)
function calcularTarifaEstimada(pesoKg, distanciaKm) {
  const peso = parseFloat(pesoKg);
  const dist = parseFloat(distanciaKm);
  if (isNaN(peso) || isNaN(dist) || peso <= 0 || dist <= 0) return null;
  return Math.max(peso * 2.5 + dist * 0.1, 5).toFixed(2);
}

export default function Paquetes() {
  const { token, puedeGestionar } = useAuth();
  const navigate = useNavigate();
  const [paquetes, setPaquetes] = useState([]);
  const [clientes, setClientes] = useState([]);
  const [filtros, setFiltros] = useState({ sucursal: '', estado: '', busqueda: '' });
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [modalCrear, setModalCrear] = useState(false);
  const [form, setForm] = useState(FORM_VACIO);
  const [erroresCampo, setErroresCampo] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback((f = {}) => {
    setCargando(true);
    api.listarPaquetes(token, f)
      .then((res) => setPaquetes(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, [token]);

  useEffect(() => { cargar(); }, [cargar]);

  useEffect(() => {
    if (puedeGestionar) {
      api.listarClientes(token)
        .then((res) => setClientes((res.data || []).filter((c) => c.activo)))
        .catch(() => {});
    }
  }, [token, puedeGestionar]);

  const nombres = useNombresClientes(token, paquetes);

  const aplicarFiltros = (e) => {
    e.preventDefault();
    cargar({
      sucursal: filtros.sucursal.trim() || undefined,
      estado: filtros.estado || undefined,
      busqueda: filtros.busqueda.trim() || undefined,
    });
  };

  const limpiar = () => {
    setFiltros({ sucursal: '', estado: '', busqueda: '' });
    cargar();
  };

  const tarifaEstimada = calcularTarifaEstimada(form.pesoKg, form.distanciaKm);

  const crear = async (e) => {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    setOk(null);
    setErroresCampo({});
    try {
      const body = {
        remitenteId: form.remitenteId,
        destinatarioId: form.destinatarioId,
        sucursalOrigen: form.sucursalOrigen,
        sucursalDestino: form.sucursalDestino,
        pesoKg: parseFloat(form.pesoKg),
        valorDeclarado: parseFloat(form.valorDeclarado),
        distanciaKm: parseFloat(form.distanciaKm),
      };
      if (form.descripcion) body.descripcion = form.descripcion;
      const res = await api.crearPaquete(token, body);
      setOk(res.message);
      setModalCrear(false);
      setForm(FORM_VACIO);
      cargar();
    } catch (err) {
      if (err.errors && typeof err.errors === 'object') {
        setErroresCampo(err.errors);
      } else {
        setError(err.message);
        setModalCrear(false);
      }
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <div className="cabecera-pagina">
        <div>
          <h1>Paquetes</h1>
          <p>Registro, seguimiento y filtrado de envíos.</p>
        </div>
        {puedeGestionar && (
          <button className="btn btn-primario" onClick={() => { setForm(FORM_VACIO); setErroresCampo({}); setModalCrear(true); }}>
            ＋ Registrar paquete
          </button>
        )}
      </div>

      {error && <div className="alerta error">{error}</div>}
      {ok && <div className="alerta ok">{ok}</div>}

      <div className="tarjeta">
        <form className="barra-filtros" onSubmit={aplicarFiltros}>
          <input
            placeholder="Buscar por código o descripción…"
            value={filtros.busqueda}
            onChange={(e) => setFiltros({ ...filtros, busqueda: e.target.value })}
          />
          <input
            placeholder="Sucursal (ej. Lima)"
            value={filtros.sucursal}
            onChange={(e) => setFiltros({ ...filtros, sucursal: e.target.value })}
          />
          <select
            value={filtros.estado}
            onChange={(e) => setFiltros({ ...filtros, estado: e.target.value })}
          >
            <option value="">Todos los estados</option>
            {ESTADOS.map((es) => <option key={es} value={es}>{es.replace('_', ' ')}</option>)}
          </select>
          <button className="btn btn-secundario btn-sm">Filtrar</button>
          <button type="button" className="btn btn-borde btn-sm" onClick={limpiar}>Limpiar</button>
        </form>

        {cargando ? (
          <div className="vacio">Cargando…</div>
        ) : paquetes.length === 0 ? (
          <div className="vacio">No se encontraron paquetes.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Remitente</th>
                <th>Destinatario</th>
                <th>Ruta</th>
                <th>Peso</th>
                <th>Tarifa</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {paquetes.map((p) => (
                <tr key={p.id} className="clicable" onClick={() => navigate(`/paquetes/${p.id}`)}>
                  <td><Link to={`/paquetes/${p.id}`} onClick={(e) => e.stopPropagation()}>{p.codigoRastreo}</Link></td>
                  <td>{p.remitenteNombre || nombres[p.remitenteId] || '—'}</td>
                  <td>{p.destinatarioNombre || nombres[p.destinatarioId] || '—'}</td>
                  <td>{p.sucursalOrigen} → {p.sucursalDestino}</td>
                  <td>{Number(p.pesoKg).toFixed(2)} kg</td>
                  <td>S/ {Number(p.tarifa).toFixed(2)}</td>
                  <td><EstadoBadge estado={p.estado} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modalCrear && (
        <Modal titulo="Registrar paquete" onCerrar={() => setModalCrear(false)}>
          <form onSubmit={crear}>
            <div className="fila-campos">
              <div className="campo">
                <label>Remitente</label>
                <select
                  value={form.remitenteId}
                  onChange={(e) => setForm({ ...form, remitenteId: e.target.value })}
                  required
                >
                  <option value="">Seleccionar cliente…</option>
                  {clientes.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombreCompleto} — {c.dni}</option>
                  ))}
                </select>
                {erroresCampo.remitenteId && <div className="error-campo">{erroresCampo.remitenteId}</div>}
              </div>
              <div className="campo">
                <label>Destinatario</label>
                <select
                  value={form.destinatarioId}
                  onChange={(e) => setForm({ ...form, destinatarioId: e.target.value })}
                  required
                >
                  <option value="">Seleccionar cliente…</option>
                  {clientes.map((c) => (
                    <option key={c.id} value={c.id}>{c.nombreCompleto} — {c.dni}</option>
                  ))}
                </select>
                {erroresCampo.destinatarioId && <div className="error-campo">{erroresCampo.destinatarioId}</div>}
              </div>
            </div>
            <div className="fila-campos">
              <div className="campo">
                <label>Sucursal de origen</label>
                <input
                  value={form.sucursalOrigen}
                  onChange={(e) => setForm({ ...form, sucursalOrigen: e.target.value })}
                  placeholder="Lima"
                  required
                  minLength={2}
                />
                {erroresCampo.sucursalOrigen && <div className="error-campo">{erroresCampo.sucursalOrigen}</div>}
              </div>
              <div className="campo">
                <label>Sucursal de destino</label>
                <input
                  value={form.sucursalDestino}
                  onChange={(e) => setForm({ ...form, sucursalDestino: e.target.value })}
                  placeholder="Arequipa"
                  required
                  minLength={2}
                />
                {erroresCampo.sucursalDestino && <div className="error-campo">{erroresCampo.sucursalDestino}</div>}
              </div>
            </div>
            <div className="fila-campos">
              <div className="campo">
                <label>Peso (kg)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  max="1000"
                  value={form.pesoKg}
                  onChange={(e) => setForm({ ...form, pesoKg: e.target.value })}
                  required
                />
                {erroresCampo.pesoKg && <div className="error-campo">{erroresCampo.pesoKg}</div>}
              </div>
              <div className="campo">
                <label>Distancia (km)</label>
                <input
                  type="number"
                  step="0.1"
                  min="0.1"
                  value={form.distanciaKm}
                  onChange={(e) => setForm({ ...form, distanciaKm: e.target.value })}
                  required
                />
                {erroresCampo.distanciaKm && <div className="error-campo">{erroresCampo.distanciaKm}</div>}
              </div>
            </div>
            <div className="campo">
              <label>Valor declarado (S/)</label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                value={form.valorDeclarado}
                onChange={(e) => setForm({ ...form, valorDeclarado: e.target.value })}
                required
              />
              {erroresCampo.valorDeclarado && <div className="error-campo">{erroresCampo.valorDeclarado}</div>}
            </div>
            <div className="campo">
              <label>Descripción (opcional)</label>
              <textarea
                rows={2}
                maxLength={500}
                value={form.descripcion}
                onChange={(e) => setForm({ ...form, descripcion: e.target.value })}
              />
            </div>

            {tarifaEstimada && (
              <div className="tarifa-preview">
                Tarifa estimada: <strong>S/ {tarifaEstimada}</strong>
                <div className="ayuda">
                  Fórmula: (peso × 2.50) + (distancia × 0.10), mínimo S/ 5.00. El backend calcula el valor final.
                </div>
              </div>
            )}

            <div className="acciones">
              <button type="button" className="btn btn-borde" onClick={() => setModalCrear(false)}>Cancelar</button>
              <button className="btn btn-primario" disabled={guardando}>
                {guardando ? 'Registrando…' : 'Registrar paquete'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
