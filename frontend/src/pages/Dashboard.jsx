import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';
import EstadoBadge from '../components/EstadoBadge.jsx';
import { useNombresClientes } from '../hooks/useNombresClientes.js';

const ESTADOS = ['REGISTRADO', 'EN_ALMACEN', 'EN_TRANSITO', 'EN_DESTINO', 'ENTREGADO', 'CANCELADO'];

export default function Dashboard() {
  const { token, sesion, esCliente } = useAuth();
  const [paquetes, setPaquetes] = useState([]);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(true);

  useEffect(() => {
    api.listarPaquetes(token)
      .then((res) => setPaquetes(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, [token]);

  const nombres = useNombresClientes(token, paquetes);
  const conteo = (estado) => paquetes.filter((p) => p.estado === estado).length;
  const recientes = [...paquetes]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 6);

  return (
    <>
      <div className="cabecera-pagina">
        <div>
          <h1>Bienvenido, {sesion?.nombre}</h1>
          <p>{esCliente ? 'Resumen de tus envíos' : 'Resumen general de la operación'}</p>
        </div>
        <Link to="/paquetes" className="btn btn-primario">Ver paquetes</Link>
      </div>

      {error && <div className="alerta error">{error}</div>}

      <div className="grid-stats">
        <div className="stat">
          <div className="valor">{paquetes.length}</div>
          <div className="etiqueta">Total de paquetes</div>
        </div>
        {ESTADOS.map((e) => (
          <div className="stat" key={e}>
            <div className="valor">{conteo(e)}</div>
            <div className="etiqueta">{e.replace('_', ' ')}</div>
          </div>
        ))}
      </div>

      <div className="tarjeta">
        <h2 style={{ fontSize: 17, marginBottom: 14 }}>Paquetes recientes</h2>
        {cargando ? (
          <div className="vacio">Cargando…</div>
        ) : recientes.length === 0 ? (
          <div className="vacio">Aún no hay paquetes registrados.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Código</th>
                <th>Remitente</th>
                <th>Ruta</th>
                <th>Tarifa</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {recientes.map((p) => (
                <tr key={p.id}>
                  <td><Link to={`/paquetes/${p.id}`}>{p.codigoRastreo}</Link></td>
                  <td>{p.remitenteNombre || nombres[p.remitenteId] || '—'}</td>
                  <td>{p.sucursalOrigen} → {p.sucursalDestino}</td>
                  <td>S/ {Number(p.tarifa).toFixed(2)}</td>
                  <td><EstadoBadge estado={p.estado} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
