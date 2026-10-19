# Bloom API

API REST de Bloom, una tienda en línea de plantas, flores y guías digitales. Maneja usuarios, productos y órdenes.

Está hecha con Java 21, Spring Boot 4, PostgreSQL 16, Flyway, Spring Security con JWT y springdoc para la documentación OpenAPI. El frontend está en el repo [blossom-ecommerce-web](https://github.com/Camygonza/blossom-ecommerce-web).

Hay una versión desplegada en https://bloom-web-ah8o.onrender.com. Como corre en el plan gratuito de Render, la primera visita tarda cerca de un minuto en responder.

## Cómo correrla

### Con Docker

Clona este repo y el del web en la misma carpeta y, desde este:

```bash
cp env.ejemplo .env     # cambia DB_PASSWORD y JWT_SECRET
docker compose up --build
```

La tienda queda en http://localhost:3000 y Swagger en http://localhost:8080/swagger-ui.html. Los dos secretos son obligatorios (compose falla si faltan) y `.env` no se sube al repo.

### Sin Docker para la API

Necesitas JDK 21 y Docker (para PostgreSQL y para los tests).

```bash
docker run -d --name bloom-db -p 5432:5432 -e POSTGRES_DB=bloom -e POSTGRES_USER=bloom -e POSTGRES_PASSWORD=bloom postgres:16-alpine
./mvnw spring-boot:run
```

Flyway crea las tablas y carga datos de ejemplo: 5 categorías y 30 productos con precios en pesos colombianos. Se pueden cambiar `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` y `LOGIN_RATE_LIMIT_MAX`; los valores por defecto son solo para desarrollo.

El contrato OpenAPI se sirve en `/v3/api-docs` y hay una copia en [docs/openapi.json](docs/openapi.json). En Swagger UI se usa el botón Authorize con el access token.

### Usuarios de ejemplo

Vienen en el seed para poder probar sin registrarse.

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@bloom.test | Admin123! |
| Cliente | cliente@bloom.test | Cliente123! |

### Despliegue en Render

[render.yaml](render.yaml) describe la base de datos, la API y el web. Se crea desde Render con New > Blueprint, conectando este repo (la cuenta de GitHub debe tener acceso también al del web). `JWT_SECRET` se genera solo y la URL de la base llega como `DATABASE_URL`, que el script de arranque convierte a JDBC. Con el plan gratuito los servicios se duermen a los 15 minutos sin tráfico y la base de datos caduca a los 30 días.

## Endpoints

Todos cuelgan de `/api/v1`.

| Método | Ruta | Quién |
|---|---|---|
| POST | `/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout` | Público |
| GET | `/users/me` | Usuario autenticado |
| GET | `/products` (filtros `name`, `categoryId`, `minPrice`, `maxPrice`; `page`, `size`, `sort`) | Público |
| GET | `/products/{id}`, `/categories` | Público |
| POST, PUT, DELETE | `/products`, `/products/{id}` | Administrador |
| POST | `/orders` | Cliente |
| GET | `/orders/me`, `/orders/{id}` (solo la dueña o el dueño) | Cliente |
| POST | `/orders/{id}/cancel` (solo órdenes pendientes, devuelve el stock) | Cliente |
| GET | `/orders` (filtro `status`) | Administrador |
| PATCH | `/orders/{id}/status` | Administrador |

Los errores siguen el formato `application/problem+json` (RFC 7807) e incluyen un `requestId`; los de validación traen además un `errors` con el mensaje de cada campo. La colección de Postman con todas las rutas va en la carpeta de entregas.

## Tests

```bash
./mvnw verify
```

Son 140 tests: unitarios con Mockito, de controladores con MockMvc y de repositorios contra un PostgreSQL real con Testcontainers. El build exige un 80 % de cobertura de líneas con JaCoCo y hoy está cerca del 97 % (informe en `target/site/jacoco/index.html`). Un test de contrato falla si la API cambia sin actualizar `docs/openapi.json`; para regenerarlo: `./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true`. El CI de GitHub Actions corre `verify` y construye la imagen.

## Decisiones de diseño

Cada una está con su alternativa descartada en [docs/decisiones.md](docs/decisiones.md). Las principales:

- Productos físicos y digitales comparten una tabla base con herencia JOINED. `OrderItem` es una entidad propia, para guardar la cantidad y el precio del momento de la compra, en lugar de un `@ManyToMany`.
- Autenticación con JWT: access token de 15 minutos y refresh token opaco que rota en cada uso. Si alguien reusa un refresh ya cambiado, se revoca toda la sesión. El login tiene límite de intentos y bloqueo temporal.
- La búsqueda usa `JpaSpecificationExecutor`, con tamaño de página máximo de 50 y orden solo por columnas permitidas. Para `LIKE '%texto%'` agregué un índice de trigramas: con 200 000 filas de prueba, la consulta pasó de unos 60 ms a unos 0,25 ms.
- Para no vender de más uso `@Version` en el producto y reintento la transacción si hay conflicto. Lo probé con hilos.
- Las órdenes pasan por `PENDING`, `PAID`, `SHIPPED` y `DELIVERED`, y se pueden cancelar antes del envío. No hay pasarela de pago, así que un administrador marca la orden como pagada.
- Pedir una orden ajena responde 404, para no revelar que existe. El catálogo no expone el `downloadUrl` de los productos digitales.
- Al crear una orden se publica un evento que un adaptador escribe en el log (simula el correo). Cada petición lleva un `X-Request-Id` que aparece en los logs y en los errores.

## Dificultades

- Con `@Version` solo, dos compras simultáneas del mismo producto hacían fallar a una de ellas aunque hubiera stock. Moví la transacción a otro bean para poder reintentarla completa, releyendo el stock en cada intento.
- Al devolver el stock de una orden cancelada, `OrderItem.product` llegaba como proxy de Hibernate de la clase abstracta y `instanceof PhysicalProduct` fallaba. Lo resolví con `Hibernate.unproxy`.
- Los 401 y 403 salen de los filtros de seguridad y no pasan por `@RestControllerAdvice`, así que escribí un handler propio para que todos los errores tengan el mismo formato.
- Detrás de nginx el límite de intentos de login veía a todos con la misma IP. Con `server.forward-headers-strategy=native` toma `X-Forwarded-For` solo de proxies internos.

## Pendientes y qué haría con más tiempo

- El límite de intentos de login vive en memoria, así que solo sirve con una instancia. Con varias habría que moverlo a Redis.
- El borrado de productos es físico y devuelve 409 si el producto ya está en una orden. Un borrado lógico conservaría el historial.
- Con más de 5 compras simultáneas del mismo producto el reintento puede agotarse. Un `UPDATE ... SET stock = stock - ? WHERE stock >= ?` atómico lo evitaría.
- Falta una pasarela de pago, correo real y un endpoint de descarga autorizada para los productos digitales.
- Los administradores no pueden ver el detalle de órdenes ajenas; solo cambian su estado desde el listado.
- Los nombres y descripciones de producto se guardan solo en español. Para tenerlos en inglés habría que agregar columnas por idioma y leer `Accept-Language`.
- En producción irían secretos gestionados, TLS y sin los usuarios de ejemplo del seed.
