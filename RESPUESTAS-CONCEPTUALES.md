# Respuestas Conceptuales - RapidoCourier

## 1. ¿Por qué elegiste la descomposición en estos microservicios?

Se eligieron 3 bounded contexts diferenciados según responsabilidades de negocio:

- **servicio-auth**: Gestión de identidad y acceso. Centraliza JWT, roles y credenciales. Justificación:
  la seguridad es una preocupación transversal que no debe acoplarse a la lógica de negocio.

- **servicio-clientes**: Gestión de personas. Encapsula la integración con RENIEC (API externa) y
  el ciclo de vida de clientes. Justificación: cambios en la API de RENIEC no deben afectar la lógica
  de paquetes.

- **servicio-paquetes**: Logística core. Contiene la mayor parte de la lógica de negocio: tarifa,
  estados, historial, categorías. Justificación: el dominio de paquetes es autónomo y podría escalar
  independientemente de los clientes.

## 2. Comunicación sincrónica vs. asincrónica

Se usó **comunicación sincrónica (Feign)** para:
- Consulta RENIEC al registrar cliente: se necesita la respuesta en tiempo real para crear el cliente.
- Consulta datos de cliente al registrar paquete: enriquecimiento inmediato de la respuesta.

Se justificaría **comunicación asincrónica (RabbitMQ/Kafka)** para:
- Notificaciones de estado: enviar email/SMS cuando cambia el estado de un paquete.
- Auditoría: registrar eventos en un sistema de log centralizado.

## 3. ¿Cómo se maneja la consistencia entre servicios?

Al registrar un paquete, se almacena el `remitenteId` y `destinatarioId` como UUID sin FK externa
(patrón *Saga sin transacción distribuida*). Si el servicio-clientes está caído, el Circuit Breaker
activa el fallback retornando 502. Esto garantiza que no hay datos inconsistentes: o se guarda todo
(con los IDs válidos) o falla con un error claro.

## 4. Decisión difícil: ¿Dónde validar JWT?

**Opciones consideradas:**
1. Validar JWT solo en API Gateway (centralizado).
2. Validar JWT en cada microservicio (distribuido).

**Decisión**: Validar en cada microservicio. **Razón**: si un cliente accede directamente a un servicio
(saltando el gateway), igual requiere token válido. Esto sigue el principio de *defense in depth*.
El trade-off es duplicación del filtro JWT en cada servicio, pero con una clase compartida el
mantenimiento es mínimo.

## 5. ¿Por qué H2 en lugar de PostgreSQL?

H2 en memoria permite arrancar el proyecto sin infraestructura adicional, ideal para examen/demo.
El schema JPA `create-drop` garantiza que siempre arranca limpio. Para producción, solo se cambia
el `datasource.url` a PostgreSQL y el `ddl-auto` a `validate` o `update`.

## 6. Circuit Breaker: ¿cuándo abre el circuito?

Configuración para `reniec-cb` (servicio-clientes):
- `failureRateThreshold: 50` → abre si el 50% de llamadas en la ventana fallan.
- `slidingWindowSize: 5` → evalúa las últimas 5 llamadas.
- `waitDurationInOpenState: 30s` → espera 30 segundos antes de pasar a HALF_OPEN.

Cuando el circuito está OPEN, todas las llamadas van directo al fallback que lanza 502, evitando
sobrecargar a RENIEC con reintentos.

## 7. Patrón de roles y restricciones (RF-08)

| Endpoint | ADMIN | OPERADOR | CLIENTE |
|---|---|---|---|
| POST /clientes | ✓ | ✓ | ✗ |
| POST /paquetes | ✓ | ✓ | ✗ |
| PATCH /paquetes/{id}/estado | ✓ | ✓ | ✗ |
| GET /paquetes | ✓ | ✓ | ✓ |
| GET /paquetes/{id}/historial | ✓ | ✓ | ✓ |
| DELETE /paquetes/{id} | ✓ | ✗ | ✗ |

Implementado con `@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")` en cada endpoint.
