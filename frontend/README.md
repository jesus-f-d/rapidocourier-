# RapidoCourier — Frontend

Interfaz web del sistema de microservicios RapidoCourier, construida con **React + Vite**.

## Funcionalidades

- **Login / Registro** de usuarios con roles ADMIN, OPERADOR y CLIENTE (JWT).
- **Dashboard** con métricas de paquetes por estado.
- **Clientes**: registro por DNI (el nombre se obtiene de RENIEC), búsqueda, edición y desactivación.
- **Paquetes**: registro con cálculo de tarifa, filtros por sucursal/estado, búsqueda por texto.
- **Detalle de paquete**: cambio de estado con transiciones validadas (RF-04), historial tipo línea de tiempo (RF-05) y asignación de categorías (RF-09).
- **Categorías**: creación y listado (solo ADMIN).
- La interfaz se adapta al rol del usuario: un CLIENTE solo ve sus propios envíos, en modo lectura.

## Configuración

La URL del API Gateway se define en `.env`:

```
VITE_API_URL=http://localhost:8080
```

## Desarrollo local

```bash
npm install
npm run dev
```

Requiere el backend levantado (Eureka, Config Server, Gateway y los tres servicios de negocio).
Ver el README de la raíz del repositorio para las instrucciones de arranque.

## Build de producción

```bash
npm run build
```

El resultado queda en `dist/`. El proyecto está listo para desplegarse en Vercel
(`vercel.json` incluye las rewrites para el enrutado SPA).

## Cuentas de demostración

| Rol | Email | Contraseña |
|---|---|---|
| ADMIN | admin@rapidocourier.pe | Admin1234 |
| OPERADOR | operador@rapidocourier.pe | Operador1234 |
| CLIENTE | cliente@rapidocourier.pe | Cliente1234 |
