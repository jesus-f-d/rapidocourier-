import { createContext, useContext, useMemo, useState } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

function leerSesion() {
  try {
    const raw = sessionStorage.getItem('rc_sesion');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [sesion, setSesion] = useState(leerSesion);

  const login = async (email, password) => {
    const res = await api.login(email, password);
    const data = res.data; // { id, email, nombre, roles, token, tokenType }
    sessionStorage.setItem('rc_sesion', JSON.stringify(data));
    setSesion(data);
    return data;
  };

  const logout = () => {
    sessionStorage.removeItem('rc_sesion');
    setSesion(null);
  };

  const value = useMemo(() => {
    const roles = sesion?.roles || [];
    return {
      sesion,
      token: sesion?.token || null,
      roles,
      esAdmin: roles.includes('ADMIN'),
      esOperador: roles.includes('OPERADOR'),
      esCliente: roles.includes('CLIENTE') && !roles.includes('ADMIN') && !roles.includes('OPERADOR'),
      puedeGestionar: roles.includes('ADMIN') || roles.includes('OPERADOR'),
      login,
      logout,
    };
  }, [sesion]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
