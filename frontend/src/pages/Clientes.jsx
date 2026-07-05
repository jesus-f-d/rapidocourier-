import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext.jsx';
import Modal from '../components/Modal.jsx';

const FORM_VACIO = { dni: '', email: '', telefono: '', direccion: '' };

export default function Clientes() {
  const { token, esAdmin } = useAuth();
  const [clientes, setClientes] = useState([]);
  const [busqueda, setBusqueda] = useState('');
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [modal, setModal] = useState(null); // null | 'crear' | cliente a editar
  const [form, setForm] = useState(FORM_VACIO);
  const [erroresCampo, setErroresCampo] = useState({});
  const [guardando, setGuardando] = useState(false);

  const cargar = useCallback((texto) => {
    setCargando(true);
    api.listarClientes(token, texto)
      .then((res) => setClientes(res.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setCargando(false));
  }, [token]);

  useEffect(() => { cargar(); }, [cargar]);

  const buscar = (e) => {
    e.preventDefault();
    cargar(busqueda.trim() || undefined);
  };

  const abrirCrear = () => {
    setForm(FORM_VACIO);
    setErroresCampo({});
    setModal('crear');
  };

  const abrirEditar = (cliente) => {
    setForm({
      email: cliente.email,
      telefono: cliente.telefono || '',
      direccion: cliente.direccion || '',
    });
    setErroresCampo({});
    setModal(cliente);
  };

  const guardar = async (e) => {
    e.preventDefault();
    setGuardando(true);
    setError(null);
    setOk(null);
    setErroresCampo({});
    try {
      if (modal === 'crear') {
        const body = { dni: form.dni, email: form.email };
        if (form.telefono) body.telefono = form.telefono;
        if (form.direccion) body.direccion = form.direccion;
        const res = await api.crearCliente(token, body);
        setOk(`Cliente registrado: ${res.data.nombreCompleto} (nombre obtenido de RENIEC)`);
      } else {
        const body = { email: form.email };
        if (form.telefono) body.telefono = form.telefono;
        if (form.direccion) body.direccion = form.direccion;
        await api.actualizarCliente(token, modal.id, body);
        setOk('Cliente actualizado exitosamente.');
      }
      setModal(null);
      cargar();
    } catch (err) {
      if (err.errors && typeof err.errors === 'object') {
        setErroresCampo(err.errors);
      } else {
        setError(err.message);
        setModal(null);
      }
    } finally {
      setGuardando(false);
    }
  };

  const desactivar = async (cliente) => {
    if (!window.confirm(`¿Desactivar al cliente ${cliente.nombreCompleto}?`)) return;
    setError(null);
    setOk(null);
    try {
      await api.desactivarCliente(token, cliente.id);
      setOk('Cliente desactivado.');
      cargar();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <div className="cabecera-pagina">
        <div>
          <h1>Clientes</h1>
          <p>El nombre completo se obtiene automáticamente de RENIEC al registrar el DNI.</p>
        </div>
        <button className="btn btn-primario" onClick={abrirCrear}>＋ Registrar cliente</button>
      </div>

      {error && <div className="alerta error">{error}</div>}
      {ok && <div className="alerta ok">{ok}</div>}

      <div className="tarjeta">
        <form className="barra-filtros" onSubmit={buscar}>
          <input
            placeholder="Buscar por DNI, nombre o email…"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
          <button className="btn btn-secundario btn-sm">Buscar</button>
          {busqueda && (
            <button
              type="button"
              className="btn btn-borde btn-sm"
              onClick={() => { setBusqueda(''); cargar(); }}
            >
              Limpiar
            </button>
          )}
        </form>

        {cargando ? (
          <div className="vacio">Cargando…</div>
        ) : clientes.length === 0 ? (
          <div className="vacio">No se encontraron clientes.</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>DNI</th>
                <th>Nombre completo</th>
                <th>Email</th>
                <th>Teléfono</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id}>
                  <td>{c.dni}</td>
                  <td>{c.nombreCompleto}</td>
                  <td>{c.email}</td>
                  <td>{c.telefono || '—'}</td>
                  <td>
                    <span className={`badge ${c.activo ? 'activo' : 'inactivo'}`}>
                      {c.activo ? 'ACTIVO' : 'INACTIVO'}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn-borde btn-sm" onClick={() => abrirEditar(c)}>Editar</button>{' '}
                    {esAdmin && c.activo && (
                      <button className="btn btn-peligro btn-sm" onClick={() => desactivar(c)}>Desactivar</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {modal && (
        <Modal
          titulo={modal === 'crear' ? 'Registrar cliente' : `Editar cliente — ${modal.nombreCompleto}`}
          onCerrar={() => setModal(null)}
        >
          <form onSubmit={guardar}>
            {modal === 'crear' && (
              <div className="campo">
                <label>DNI</label>
                <input
                  value={form.dni}
                  onChange={(e) => setForm({ ...form, dni: e.target.value })}
                  maxLength={8}
                  pattern="\d{8}"
                  placeholder="8 dígitos"
                  required
                />
                <div className="ayuda">Se consultará RENIEC para obtener el nombre completo.</div>
                {erroresCampo.dni && <div className="error-campo">{erroresCampo.dni}</div>}
              </div>
            )}
            <div className="campo">
              <label>Email</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
              {erroresCampo.email && <div className="error-campo">{erroresCampo.email}</div>}
            </div>
            <div className="fila-campos">
              <div className="campo">
                <label>Teléfono (opcional)</label>
                <input
                  value={form.telefono}
                  onChange={(e) => setForm({ ...form, telefono: e.target.value })}
                  placeholder="9XXXXXXXX"
                  maxLength={9}
                />
                {erroresCampo.telefono && <div className="error-campo">{erroresCampo.telefono}</div>}
              </div>
              <div className="campo">
                <label>Dirección (opcional)</label>
                <input
                  value={form.direccion}
                  onChange={(e) => setForm({ ...form, direccion: e.target.value })}
                  maxLength={250}
                />
                {erroresCampo.direccion && <div className="error-campo">{erroresCampo.direccion}</div>}
              </div>
            </div>
            <div className="acciones">
              <button type="button" className="btn btn-borde" onClick={() => setModal(null)}>Cancelar</button>
              <button className="btn btn-primario" disabled={guardando}>
                {guardando ? 'Guardando…' : 'Guardar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  );
}
