// Cliente HTTP para consumir el API Gateway de RapidoCourier
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export class ApiError extends Error {
  constructor(message, status, errors) {
    super(message);
    this.status = status;
    this.errors = errors;
  }
}

async function request(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${API_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError('No se pudo conectar con el servidor. Verifica que el API Gateway esté activo.', 0);
  }

  if (res.status === 204) return null;

  let json = null;
  try {
    json = await res.json();
  } catch {
    // respuesta sin cuerpo JSON
  }

  if (!res.ok) {
    const message = json?.message || `Error ${res.status}`;
    throw new ApiError(message, res.status, json?.errors);
  }
  return json;
}

export const api = {
  // ---- Auth ----
  login: (email, password) =>
    request('/api/v1/auth/login', { method: 'POST', body: { email, password } }),
  register: (data) =>
    request('/api/v1/auth/register', { method: 'POST', body: data }),

  // ---- Clientes ----
  listarClientes: (token, busqueda) =>
    request(`/api/v1/clientes${busqueda ? `?busqueda=${encodeURIComponent(busqueda)}` : ''}`, { token }),
  obtenerCliente: (token, id) => request(`/api/v1/clientes/${id}`, { token }),
  obtenerClientePorDni: (token, dni) => request(`/api/v1/clientes/dni/${dni}`, { token }),
  crearCliente: (token, data) => request('/api/v1/clientes', { method: 'POST', body: data, token }),
  actualizarCliente: (token, id, data) =>
    request(`/api/v1/clientes/${id}`, { method: 'PUT', body: data, token }),
  desactivarCliente: (token, id) =>
    request(`/api/v1/clientes/${id}`, { method: 'DELETE', token }),

  // ---- Paquetes ----
  listarPaquetes: (token, { sucursal, estado, busqueda } = {}) => {
    const params = new URLSearchParams();
    if (sucursal) params.set('sucursal', sucursal);
    if (estado) params.set('estado', estado);
    if (busqueda) params.set('busqueda', busqueda);
    const qs = params.toString();
    return request(`/api/v1/paquetes${qs ? `?${qs}` : ''}`, { token });
  },
  obtenerPaquete: (token, id) => request(`/api/v1/paquetes/${id}`, { token }),
  crearPaquete: (token, data) => request('/api/v1/paquetes', { method: 'POST', body: data, token }),
  actualizarPaquete: (token, id, data) =>
    request(`/api/v1/paquetes/${id}`, { method: 'PUT', body: data, token }),
  cambiarEstado: (token, id, nuevoEstado, observacion) =>
    request(`/api/v1/paquetes/${id}/estado`, {
      method: 'PATCH',
      body: { nuevoEstado, observacion: observacion || undefined },
      token,
    }),
  obtenerHistorial: (token, id) => request(`/api/v1/paquetes/${id}/historial`, { token }),
  eliminarPaquete: (token, id) => request(`/api/v1/paquetes/${id}`, { method: 'DELETE', token }),
  asignarCategoria: (token, paqueteId, categoriaId) =>
    request(`/api/v1/paquetes/${paqueteId}/categorias/${categoriaId}`, { method: 'POST', token }),

  // ---- Categorías ----
  listarCategorias: (token) => request('/api/v1/categorias', { token }),
  crearCategoria: (token, nombre, descripcion) => {
    const params = new URLSearchParams({ nombre });
    if (descripcion) params.set('descripcion', descripcion);
    return request(`/api/v1/categorias?${params.toString()}`, { method: 'POST', token });
  },
};

export { API_URL };
