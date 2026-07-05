import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Dashboard from './pages/Dashboard.jsx';
import Clientes from './pages/Clientes.jsx';
import Paquetes from './pages/Paquetes.jsx';
import PaqueteDetalle from './pages/PaqueteDetalle.jsx';
import Categorias from './pages/Categorias.jsx';

function RutaProtegida({ children }) {
  const { token } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  return <Layout>{children}</Layout>;
}

export default function App() {
  const { token, puedeGestionar, esAdmin } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={token ? <Navigate to="/" replace /> : <Login />} />
      <Route path="/" element={<RutaProtegida><Dashboard /></RutaProtegida>} />
      <Route
        path="/clientes"
        element={
          <RutaProtegida>
            {puedeGestionar ? <Clientes /> : <Navigate to="/" replace />}
          </RutaProtegida>
        }
      />
      <Route path="/paquetes" element={<RutaProtegida><Paquetes /></RutaProtegida>} />
      <Route path="/paquetes/:id" element={<RutaProtegida><PaqueteDetalle /></RutaProtegida>} />
      <Route
        path="/categorias"
        element={
          <RutaProtegida>
            {esAdmin ? <Categorias /> : <Navigate to="/" replace />}
          </RutaProtegida>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
