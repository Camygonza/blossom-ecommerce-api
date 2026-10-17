# Bloom · API

API REST del e-commerce **Bloom** (plantas, flores y guías digitales): usuarios, productos y órdenes.
Java 21 · Spring Boot 4 · PostgreSQL 16 · Flyway · Spring Security + JWT · springdoc (OpenAPI).

## Cómo correrla

Requisitos: JDK 21 y Docker (para PostgreSQL y para los tests).

```bash
docker run -d --name bloom-db -p 5432:5432 -e POSTGRES_DB=bloom -e POSTGRES_USER=bloom -e POSTGRES_PASSWORD=bloom postgres:16-alpine
./mvnw spring-boot:run
```

Flyway crea el esquema y carga datos de demostración (30 productos, 5 categorías). Variables opcionales: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` (los valores por defecto sirven solo para desarrollo).

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Contrato OpenAPI: `GET /v3/api-docs` · copia fijada en [`docs/openapi.json`](docs/openapi.json)

Usuarios de demostración (solo desarrollo): `admin@bloom.test` / `Admin123!` y `cliente@bloom.test` / `Cliente123!`.

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
| PATCH | `/orders/{id}/status` | ADMIN |

Los errores son `application/problem+json` (RFC 7807) con `requestId`; los de validación añaden `errors` (campo → mensaje).

## Tests

```bash
./mvnw verify   # unitarios (Mockito), MockMvc y repositorios contra PostgreSQL real (Testcontainers)
```

JaCoCo exige ≥ 80 % de líneas en el build (hoy ≈ 97 %). Informe: `target/site/jacoco/index.html`. Una prueba de contrato falla si la API cambia sin regenerar `docs/openapi.json` (`./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true`).

## Decisiones de diseño

- **Herencia JOINED** (`Product` → `PhysicalProduct` / `DigitalProduct`) y **`OrderItem`** como entidad (cantidad y precio congelado) en vez de `@ManyToMany`.
- **JWT stateless**: access de 15 min + refresh opaco con rotación y detección de reuso (hash en BD); rate limit y bloqueo temporal en login.
- **Búsqueda** con `JpaSpecificationExecutor`; paginación acotada (`size ≤ 50`) y orden con lista blanca. Índice de trigramas para `LIKE '%texto%'`: de ~60 ms a ~0,25 ms con 200 000 filas.
- **Sin sobreventa**: `@Version` en `Product` más reintento de la transacción ante conflicto; probado con hilos.
- **Estados de la orden** acotados (`PENDING → PAID → SHIPPED → DELIVERED`, cancelación antes del envío con reposición de stock). No hay pasarela de pago: `PAID` lo marca un administrador.
- Una orden ajena responde **404** (no revela que existe). El catálogo no expone `downloadUrl` de los productos digitales.
- Notificación de órdenes mediante un puerto con un adaptador simulado que escribe en el log; `X-Request-Id` en logs y errores.

## Límites conocidos

- El limitador de intentos de login está en memoria (una instancia); con réplicas habría que moverlo a un almacén compartido.
- El borrado de productos es físico y se rechaza (409) si figuran en órdenes.
- Con más de 5 compras simultáneas del mismo producto el reintento puede agotarse y responder 409.
