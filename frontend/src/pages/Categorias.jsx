import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';

export default function Categorias() {
  const { token } = useAuth();
  const [categorias, setCategorias] = useState([]);
  const [nombre, setNombre] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(null);
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback(() => {
    api.listarCategorias(token)
      .then((res) => setCategorias(res.data || []))
      .catch((err) => setError(err.message));
  }, [token]);

  useEffect(() => { cargar(); }, [cargar]);

  const crear = async (e) => {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    setOk(null);
    try {
      await api.crearCategoria(token, nombre.trim(), descripcion.trim() || undefined);
      setOk('Categoría creada exitosamente.');
      setNombre('');
      setDescripcion('');
      cargar();
    } catch (err) {
      setError(err.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <>
      <div className="cabecera-pagina">
        <div>
          <h1>Categorías</h1>
          <p>Clasificación de paquetes (relación muchos a muchos, RF-09).</p>
        </div>
      </div>

      {error && <div className="alerta error">{error}</div>}
      {ok && <div className="alerta ok">{ok}</div>}

      <div className="tarjeta">
        <h2 style={{ fontSize: 17, marginBottom: 14 }}>Nueva categoría</h2>
        <form onSubmit={crear}>
          <div className="fila-campos">
            <div className="campo">
              <label>Nombre</label>
              <input
                value={nombre}
                onChange={(e) => setNombre(e.target.value)}
                maxLength={50}
                placeholder="Ej. Frágil, Electrónica, Documentos…"
                required
              />
            </div>
            <div className="campo">
              <label>Descripción (opcional)</label>
              <input
                value={descripcion}
                onChange={(e) => setDescripcion(e.target.value)}
                maxLength={200}
              />
            </div>
          </div>
          <button className="btn btn-primario" disabled={guardando}>
            {guardando ? 'Creando…' : 'Crear categoría'}
          </button>
        </form>
      </div>

      <div className="tarjeta">
        <h2 style={{ fontSize: 17, marginBottom: 14 }}>Categorías existentes</h2>
        {categorias.length === 0 ? (
          <div className="vacio">Aún no hay categorías.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Descripción</th>
              </tr>
            </thead>
            <tbody>
              {categorias.map((c) => (
                <tr key={c.id}>
                  <td><span className="chip">{c.nombre}</span></td>
                  <td>{c.descripcion || '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
