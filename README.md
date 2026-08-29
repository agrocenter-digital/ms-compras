# AgroCenter Digital - ms-compras

Microservicio responsable de registrar ordenes de compra a proveedores y solicitar
el reabastecimiento correspondiente a `ms-inventario`. Conserva sus datos en
`db_compras` y nunca accede a tablas ni repositorios de inventario.

## Arquitectura y flujo

```text
React -> API Gateway -> bff-web :8080 -> ms-compras :8083
                                          |-> db_compras
                                          `-> ms-inventario :8081 (REST)
```

Al crear una orden, el servicio:

1. valida proveedor, detalles, precios y productos duplicados;
2. calcula subtotales y total con `BigDecimal`;
3. persiste la orden como `PENDIENTE` en una transaccion local;
4. registra cada entrada mediante `POST /api/inventario/stock/entrada` y la
   referencia idempotente `COMPRA-<id>`;
5. cambia la orden a `COMPLETADA` solo si todas las entradas fueron confirmadas;
6. ante un fallo, compensa las entradas confirmadas con
   `COMPENSACION-COMPRA-<id>` y deja la orden `CANCELADA`.

No existe una transaccion JPA distribuida. Si una respuesta de inventario se pierde
despues de aplicar una entrada, la orden queda cancelada con una marca interna de
conciliacion pendiente y se registra un error sin incluir tokens. Las referencias
idempotentes permiten revisar o repetir la operacion de forma controlada.

## Seguridad

`ms-compras` es un OAuth2 Resource Server stateless. No emite tokens, no almacena
passwords y no habilita CORS para navegadores.

- En produccion acepta exclusivamente JWT RS256, valida firma mediante JWKS,
  `iss`, expiracion, `nbf`, audience o `client_id`, y exige `token_use=access`.
- Los grupos `cognito:groups`, el claim `custom:role` y los scopes OAuth se
  convierten en authorities. Solo `ADMIN` puede usar el contrato consumido por el
  BFF; tambien se admiten los scopes internos `compras.read` y `compras.write`.
- En `dev` valida JWT HS256 con un secret local de al menos 32 caracteres. Puede
  reutilizarse el token emitido por `ms-inventario` si BFF, inventario y compras
  comparten secret, issuer y audience.
- El JWT autenticado se propaga a inventario. Opcionalmente puede configurarse un
  token de servicio con minimo privilegio mediante `INVENTORY_SERVICE_TOKEN`.

## Endpoints

| Metodo | Ruta | Acceso | Resultado |
|---|---|---|---|
| `POST` | `/api/compras` | `ADMIN` o scope `compras.write` | Crea y reabastece una orden; responde `201` |
| `GET` | `/api/compras` | `ADMIN` o scope de compras | Lista ordenes de mas nueva a mas antigua |
| `GET` | `/api/compras/{id}` | `ADMIN` o scope de compras | Consulta una orden; responde `404` si no existe |
| `GET` | `/actuator/health` | Publico dentro de la red | Salud basica, sin detalles |
| `GET` | `/swagger-ui.html` | Desarrollo | Documentacion interactiva |
| `GET` | `/v3/api-docs` | Desarrollo | Contrato OpenAPI JSON |

El contrato JSON de compras conserva los campos que usa `bff-web`:

```json
{
  "id": 25,
  "proveedorId": 7,
  "fechaCreacion": "2026-08-29T16:00:00",
  "estado": "COMPLETADA",
  "total": 200.00,
  "detalles": [
    {
      "id": 41,
      "productoId": 10,
      "cantidad": 2,
      "precioUnitario": 100.00,
      "subtotal": 200.00
    }
  ]
}
```

Los errores `400`, `401`, `403`, `404`, `409`, `500`, `502` y `503` tienen un
JSON uniforme con `code`, `message`, `path`, `correlationId` y errores de
validacion, sin stack traces ni detalles de infraestructura.

## Variables de entorno

| Variable | Uso | Valor local |
|---|---|---|
| `SERVER_PORT` | Puerto HTTP | `8083` |
| `DB_URL` | JDBC de `db_compras` | `jdbc:postgresql://localhost:5432/db_compras` |
| `DB_USERNAME` | Usuario PostgreSQL | `postgres` |
| `DB_PASSWORD` | Password PostgreSQL | obligatorio, sin default |
| `INVENTORY_SERVICE_URL` | URL base de inventario | `http://localhost:8081` |
| `INVENTORY_CONNECT_TIMEOUT` | Timeout de conexion | `2s` |
| `INVENTORY_READ_TIMEOUT` | Timeout de respuesta | `3s` |
| `INVENTORY_SERVICE_TOKEN` | Bearer interno opcional | vacio; propaga el JWT recibido |
| `COGNITO_ISSUER_URI` | Issuer del User Pool | obligatorio en `prod` |
| `COGNITO_JWK_SET_URI` | Endpoint JWKS | obligatorio en `prod` |
| `COGNITO_AUDIENCE` | Audience/App Client esperado | `agrocenter-api` local |
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `dev` o `prod` |
| `DEV_JWT_SECRET` | Secret HS256 local de 32+ caracteres | obligatorio en `dev` |
| `DEV_JWT_ISSUER` | Issuer local compartido | `http://localhost:8081/dev-issuer` |
| `SWAGGER_ENABLED` | OpenAPI/Swagger | `true` local |
| `JPA_SHOW_SQL` | SQL en logs | `false` |

`.env.example` contiene solo valores ficticios. Nunca confirmar `.env`, tokens,
passwords, claves de Cognito o credenciales AWS.

## Ejecucion local

Requisitos: Java 21, PostgreSQL y `ms-inventario` en `http://localhost:8081`.

```powershell
$env:DB_PASSWORD='tu-clave-local'
$env:SPRING_PROFILES_ACTIVE='dev'
$env:DEV_JWT_SECRET='una-clave-local-compartida-de-al-menos-32-caracteres'
$env:DEV_JWT_ISSUER='http://localhost:8081/dev-issuer'
$env:COGNITO_AUDIENCE='agrocenter-api'
./mvnw.cmd spring-boot:run
```

Flyway crea el esquema de `db_compras`. El servicio queda en
`http://localhost:8083`; el BFF debe usar `MS_COMPRAS_URL=http://localhost:8083`.

## Docker Compose

```powershell
Copy-Item .env.example .env
# Reemplazar POSTGRES_PASSWORD y DEV_JWT_SECRET.
docker compose up --build -d
docker compose ps
curl.exe http://localhost:8083/actuator/health
```

Compose levanta solamente `ms-compras` y su PostgreSQL dedicado. Inventario debe
estar iniciado por separado. La imagen usa Java 21, usuario no-root, filesystem de
solo lectura, capabilities eliminadas y health check.

## Pruebas

```powershell
./mvnw.cmd clean verify
```

La suite usa H2 y un servidor HTTP simulado; no requiere AWS, PostgreSQL ni
microservicios externos. Cubre validacion, seguridad `401/403`, contrato BFF,
persistencia, transiciones de estado, compensacion, contrato HTTP de inventario,
timeouts, conversion de roles/scopes y carga del `ApplicationContext`.
