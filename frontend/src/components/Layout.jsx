import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext.jsx';

export default function Layout({ children }) {
  const { sesion, roles, logout, puedeGestionar, esAdmin } = useAuth();

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="logo">Rapido<span>Courier</span></div>
        <nav>
          <NavLink to="/" end className={({ isActive }) => (isActive ? 'activo' : '')}>
            Dashboard
          </NavLink>
          {puedeGestionar && (
            <NavLink to="/clientes" className={({ isActive }) => (isActive ? 'activo' : '')}>
              Clientes
            </NavLink>
          )}
          <NavLink to="/paquetes" className={({ isActive }) => (isActive ? 'activo' : '')}>
            Paquetes
          </NavLink>
          {esAdmin && (
            <NavLink to="/categorias" className={({ isActive }) => (isActive ? 'activo' : '')}>
              Categorías
            </NavLink>
          )}
        </nav>
        <div className="usuario">
          <div className="nombre">{sesion?.nombre}</div>
          <div>{sesion?.email}</div>
          {roles.map((r) => (
            <span key={r} className="rol">{r}</span>
          ))}
          <button onClick={logout}>Cerrar sesión</button>
        </div>
      </aside>
      <main className="contenido">{children}</main>
    </div>
  );
}
