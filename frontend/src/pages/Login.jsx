import { useState } from 'react';
import { useAuth } from '../context/AuthContext.jsx';
import { api } from '../api/client';

export default function Login() {
  const { login } = useAuth();
  const [modo, setModo] = useState('login'); // 'login' | 'registro'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [nombre, setNombre] = useState('');
  const [rol, setRol] = useState('CLIENTE');
  const [error, setError] = useState(null);
  const [ok, setOk] = useState(null);
  const [cargando, setCargando] = useState(false);

  const manejarSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setOk(null);
    setCargando(true);
    try {
      if (modo === 'login') {
        await login(email, password);
      } else {
        await api.register({ nombre, email, password, rol });
        setOk('Usuario registrado. Ahora puedes iniciar sesión.');
        setModo('login');
      }
    } catch (err) {
      const detalles = err.errors ? ' — ' + Object.values(err.errors).join(' | ') : '';
      setError(err.message + detalles);
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="pantalla-login">
      <div className="caja-login">
        <div className="marca">
          <h1>📦 Rapido<span>Courier</span></h1>
          <p>Sistema de gestión de envíos y paquetería</p>
        </div>

        {error && <div className="alerta error">{error}</div>}
        {ok && <div className="alerta ok">{ok}</div>}

        <form onSubmit={manejarSubmit}>
          {modo === 'registro' && (
            <div className="campo">
              <label>Nombre completo</label>
              <input value={nombre} onChange={(e) => setNombre(e.target.value)} required minLength={2} />
            </div>
          )}
          <div className="campo">
            <label>Correo electrónico</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="usuario@rapidocourier.pe"
              required
            />
          </div>
          <div className="campo">
            <label>Contraseña</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
              minLength={8}
            />
            {modo === 'registro' && (
              <div className="ayuda">Mínimo 8 caracteres, con mayúsculas, minúsculas y números.</div>
            )}
          </div>
          {modo === 'registro' && (
            <div className="campo">
              <label>Rol</label>
              <select value={rol} onChange={(e) => setRol(e.target.value)}>
                <option value="CLIENTE">CLIENTE</option>
                <option value="OPERADOR">OPERADOR</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
          )}
          <button className="btn btn-primario" disabled={cargando}>
            {cargando ? 'Procesando…' : modo === 'login' ? 'Iniciar sesión' : 'Crear cuenta'}
          </button>
        </form>

        <div className="enlace-alterno">
          {modo === 'login' ? (
            <>¿No tienes cuenta? <button onClick={() => { setModo('registro'); setError(null); }}>Regístrate</button></>
          ) : (
            <>¿Ya tienes cuenta? <button onClick={() => { setModo('login'); setError(null); }}>Inicia sesión</button></>
          )}
        </div>

        <div className="credenciales-demo">
          <strong>Cuentas de demostración:</strong><br />
          Admin: <code>admin@rapidocourier.pe</code> / <code>Admin1234</code><br />
          Operador: <code>operador@rapidocourier.pe</code> / <code>Operador1234</code><br />
          Cliente: <code>cliente@rapidocourier.pe</code> / <code>Cliente1234</code>
        </div>
      </div>
    </div>
  );
}
