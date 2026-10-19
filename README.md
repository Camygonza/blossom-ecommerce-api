# Bloom · API

API REST del e-commerce **Bloom** (plantas, flores y guías digitales): usuarios, productos y órdenes.
Java 21 · Spring Boot 4 · PostgreSQL 16 · Flyway · Spring Security + JWT · springdoc (OpenAPI).

El frontend vive en el repo hermano [`blossom-ecommerce-web`](https://github.com/Camygonza/blossom-ecommerce-web).

## Cómo correrla

### Con Docker (PostgreSQL + API + Web)

Clona los dos repos uno junto al otro y, desde este:

```bash
cp env.ejemplo .env        # y cambia DB_PASSWORD y JWT_SECRET
docker compose up --build
```

- Tienda: <http://localhost:3000> · Swagger UI: <http://localhost:8080/swagger-ui.html>
- Los secretos son obligatorios: `docker compose` falla si faltan. `.env` no se versiona.


### En Render (despliegue en la nube)

El repositorio trae un Blueprint ([`render.yaml`](render.yaml)) que crea PostgreSQL, la API y el web (este se construye desde su propio repositorio):

1. En [render.com](https://render.com): **New → Blueprint**, conecta GitHub y elige este repositorio (la cuenta de GitHub necesita acceso a este y al del web).
2. Render crea `bloom-db`, `bloom-api` y `bloom-web`; `JWT_SECRET` se genera solo y `DATABASE_URL` llega desde la base de datos (el arranque la convierte a JDBC).
3. La tienda queda en `https://bloom-web.onrender.com` (si un nombre está ocupado, cámbialo en `render.yaml`, también en `API_UPSTREAM`, `API_HOST` y `CORS_ALLOWED_ORIGINS`).

Notas del plan gratuito: los servicios se duermen tras 15 minutos sin tráfico (la primera visita tarda cerca de un minuto) y la base de datos gratuita caduca a los 30 días. Los datos de demostración del seed incluyen las cuentas de la tabla anterior.

### Local (sin Docker para la API)

Requisitos: JDK 21 y Docker (para PostgreSQL y para los tests).

```bash
docker run -d --name bloom-db -p 5432:5432 -e POSTGRES_DB=bloom -e POSTGRES_USER=bloom -e POSTGRES_PASSWORD=bloom postgres:16-alpine
./mvnw spring-boot:run
```

Flyway crea el esquema y carga datos de demostración (30 productos, 5 categorías). Variables opcionales: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `LOGIN_RATE_LIMIT_MAX` (los valores por defecto sirven solo para desarrollo).

- Swagger UI: <http://localhost:8080/swagger-ui.html> (botón **Authorize** con el access token)
- Contrato OpenAPI: `GET /v3/api-docs` · copia fijada en [`docs/openapi.json`](docs/openapi.json)

Usuarios de demostración (solo desarrollo, vienen en el seed; cámbialos o elimínalos fuera de desarrollo):

| Rol | Correo | Contraseña |
|---|---|---|
| ADMIN | `admin@bloom.test` | `Admin123!` |
| CUSTOMER | `cliente@bloom.test` | `Cliente123!` |

## Endpoints (`/api/v1`)

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | Público |
| GET | `/users/me` | Autenticado |
| GET | `/products` (`name`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size`, `sort`) | Público |
| GET | `/products/{id}`, `/categories` | Público |
| POST · PUT · DELETE | `/products`, `/products/{id}` | ADMIN |
| POST | `/orders` | CUSTOMER |
| GET | `/orders/me`, `/orders/{id}` (solo el dueño) | CUSTOMER |
| POST | `/orders/{id}/cancel` (solo `PENDING`, repone stock) | CUSTOMER |
| GET | `/orders` (filtro `status`) | ADMIN |
| PATCH | `/orders/{id}/status` | ADMIN |

Los errores son `application/problem+json` (RFC 7807) con `requestId`; los de validación añaden `errors` (campo → mensaje). La colección de Postman con todas las rutas se entrega aparte (carpeta de evidencias).

## Tests y calidad

```bash
./mvnw verify   # unitarios (Mockito), MockMvc y repositorios contra PostgreSQL real (Testcontainers)
```

- 140 tests; JaCoCo exige ≥ 80 % de líneas en el build (hoy ≈ 97 %). Informe: `target/site/jacoco/index.html`.
- Una prueba de contrato falla si la API cambia sin regenerar `docs/openapi.json` (`./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true`).
- CI (GitHub Actions): `verify` + construcción de la imagen. Los E2E del frontend corren contra este mismo `docker compose`.

## Decisiones de diseño

- **Herencia JOINED** (`Product` → `PhysicalProduct` / `DigitalProduct`) y **`OrderItem`** como entidad (cantidad y precio congelado) en vez de `@ManyToMany`.
- **JWT stateless**: access de 15 min + refresh opaco con rotación y detección de reuso (hash en BD); rate limit y bloqueo temporal en login.
- **Búsqueda** con `JpaSpecificationExecutor`; paginación acotada (`size ≤ 50`) y orden con lista blanca. Índice de trigramas para `LIKE '%texto%'`: de ~60 ms a ~0,25 ms con 200 000 filas (medido con `EXPLAIN` sobre 200 000 filas de prueba).
- **Sin sobreventa**: `@Version` en `Product` más reintento de la transacción ante conflicto; probado con hilos.
- **Estados de la orden** acotados (`PENDING → PAID → SHIPPED → DELIVERED`, cancelación antes del envío con reposición de stock). No hay pasarela de pago: `PAID` lo marca un administrador.
- Una orden ajena responde **404** (no revela que existe). El catálogo no expone `downloadUrl` de los productos digitales.
- Notificación de órdenes mediante un puerto con un adaptador simulado que escribe en el log; `X-Request-Id` en logs y errores.

Cada decisión con su alternativa descartada está en [`docs/decisiones.md`](docs/decisiones.md).

## Retos

- **Concurrencia en el stock:** el `@Version` evita vender de más, pero hacía fallar a compradores legítimos. Se movió la transacción a otro bean para reintentarla entera (cada intento relee el stock) y se probó con hilos contra PostgreSQL.
- **Proxies de Hibernate con herencia:** al reponer stock, `OrderItem.product` es un proxy de la clase abstracta y `instanceof PhysicalProduct` fallaba; se resuelve con `Hibernate.unproxy`.
- **Seguridad con ProblemDetail:** los 401/403 de los filtros no pasan por `@RestControllerAdvice`; se escriben con un handler propio para que todos los errores tengan el mismo formato.
- **Búsqueda rápida:** `LIKE '%texto%'` no usa un btree; se midió con `EXPLAIN` y se resolvió con un índice de trigramas.
- **Contrato estable:** una prueba compara `/v3/api-docs` con `docs/openapi.json`, así el front no se desalinea sin que el build lo note.
- **IP real tras el proxy:** con nginx delante, el rate limit veía a todos como una sola IP; `server.forward-headers-strategy=native` toma `X-Forwarded-For` solo de proxies internos.

## Límites conocidos y qué haría con más tiempo

- El limitador de intentos de login está en memoria (una instancia); con réplicas habría que moverlo a un almacén compartido (Redis).
- El borrado de productos es físico y se rechaza (409) si figuran en órdenes; un borrado lógico sería lo siguiente si hace falta conservarlos.
- Con más de 5 compras simultáneas del mismo producto el reintento puede agotarse y responder 409; un `UPDATE … SET stock = stock - ? WHERE stock >= ?` atómico lo evitaría.
- Sin pasarela de pago ni correo real (la notificación es un adaptador simulado): son los puntos de extensión naturales.
- Quien compra un producto digital no recibe el `downloadUrl` en la respuesta: haría falta un endpoint de descarga autorizado.
- Despliegue: el compose es para desarrollo/demostración; en producción irían secretos gestionados, TLS delante y las credenciales demo del seed eliminadas.
- El ADMIN cambia el estado de una orden por id desde el listado, pero no ve el detalle de órdenes ajenas.
