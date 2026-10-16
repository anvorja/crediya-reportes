# crediya-reportes

Servicio de **reportes** de CrediYa. Cuenta las solicitudes de préstamo **aprobadas** y suma su monto, y lo expone a los administradores.

- **Entrada de datos:** el servicio de solicitudes publica un aviso `SOLICITUD_APROBADA` en la cola SQS `crediya-solicitud-aprobada` cada vez que una solicitud queda aprobada (decisión automática o de un asesor).
- **Almacenamiento:** un acumulado en una tabla de DynamoDB (`crediya-reportes`), sin base de datos relacional.
- **Consulta:** `GET /api/v1/reportes/solicitudes-aprobadas`, solo con rol **ADMIN**.

```
solicitudes ──outbox──► SQS crediya-solicitud-aprobada ──► reportes ──► DynamoDB crediya-reportes
                                                              ▲
                                       admin ── JWT ── GET /api/v1/reportes/solicitudes-aprobadas
```

## API

| Método | Ruta | Rol | Respuesta |
|---|---|---|---|
| `GET` | `/api/v1/reportes/solicitudes-aprobadas` | ADMIN | `200` `{"total_aprobadas": 3, "monto_total_aprobado": 7500000.50}` |
| `GET` | `/actuator/health/readiness` | público | estado del servicio |
| `GET` | `/swagger-ui.html` | público | documentación OpenAPI |

Errores: `401 NO_AUTENTICADO` (sin token o inválido) y `403 ACCESO_DENEGADO` (rol distinto de ADMIN). Mientras no haya aprobaciones el reporte es de ceros, no un error.
El token lo emite `crediya-auth`; aquí solo se valida con su JWKS (`AUTH_JWKS_URI`), emisor y audiencia.

## Cómo cuenta sin contar de más

SQS entrega los mensajes **al menos una vez**, así que el mismo aviso puede llegar repetido. Por cada solicitud se escribe, en **una sola transacción** de DynamoDB:

1. una marca `APROBACION#<id_solicitud>` con la condición `attribute_not_exists(pk)` y un TTL (`expira_en`, 30 días por defecto), y
2. la suma al acumulado `RESUMEN#APROBADAS` (`ADD total_aprobadas 1, monto_total_aprobado :monto`).

Si la marca ya existía, la transacción se cancela y el acumulado no cambia; el mensaje se da por procesado. Un mensaje que rompe el contrato falla y, tras los reintentos de la cola, va a su DLQ.

Contrato del mensaje (versión 1; se ignoran los campos que no se conocen):

```json
{"version":1,"id_evento":"…","tipo":"SOLICITUD_APROBADA","ocurrido_en":"2026-10-16T12:00:00Z",
 "solicitud":{"id_solicitud":"…","monto":2000000,"plazo":24,"tipo_prestamo":"PERSONAL"}}
```

## Arquitectura

Clean Architecture (plugin Bancolombia): `domain/model` y `domain/usecase` sin dependencias de infraestructura; `infrastructure/entry-points/{reactive-web,sqs-listener}` y `infrastructure/driven-adapters/dynamo-db`.
Casos de uso: `RegistrarAprobacionUseCase` y `ObtenerReporteUseCase`.

## Configuración

| Variable | Por defecto | Uso |
|---|---|---|
| `AUTH_JWKS_URI` | `http://localhost:8081/.well-known/jwks.json` | claves públicas para validar el JWT |
| `JWT_ISSUER`, `JWT_AUDIENCE` | `crediya-auth`, `crediya` | emisor y audiencia esperados |
| `AWS_REGION` | `us-east-1` | región |
| `AWS_ENDPOINT_URL` | vacío | solo LocalStack; en AWS se deja vacío y se usa el rol de la tarea |
| `DYNAMODB_TABLE` | `crediya-reportes` | tabla (clave `pk` de texto, TTL en `expira_en`) |
| `DEDUP_TTL_DIAS` | `30` | cuánto se recuerda una solicitud ya contada |
| `SQS_APROBADAS_URL` | cola de LocalStack | cola de solicitudes aprobadas |
| `SQS_LISTENER_ENABLED` | `true` | activa o no el consumo de la cola |

No hay claves de acceso en el código ni en la configuración: en AWS las credenciales las da el rol de la tarea de ECS (`crediya-task-reportes`: `sqs:ReceiveMessage/DeleteMessage/GetQueueAttributes` sobre la cola y `dynamodb:GetItem/UpdateItem/PutItem` sobre la tabla).

## Ejecutar en local

Necesita Docker y el repositorio `microservicio-autenticacion` clonado al lado (para obtener un token de ADMIN).

```bash
./scripts/init-env.sh                      # crea .env con credenciales aleatorias y una clave JWT nueva
docker compose --env-file .env -f deployment/docker-compose.yml up --build
```

Levanta LocalStack (SQS y DynamoDB), crea la cola y la tabla, y arranca `autenticacion` y `reportes` (`http://localhost:8080`). Para probarlo:

```bash
# 1) token de ADMIN (la contraseña está en BOOTSTRAP_ADMIN_PASSWORD de tu .env)
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/login -H 'Content-Type: application/json' \
  -d "{\"correo_electronico\":\"admin@crediya.local\",\"password\":\"$(grep ^BOOTSTRAP_ADMIN_PASSWORD .env | cut -d= -f2)\"}" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

# 2) simular un aviso de aprobación
docker compose --env-file .env -f deployment/docker-compose.yml run --rm --no-deps -T --entrypoint aws aws-init \
  --endpoint-url http://localstack:4566 sqs send-message \
  --queue-url http://localstack:4566/000000000000/crediya-solicitud-aprobada \
  --message-body '{"version":1,"solicitud":{"id_solicitud":"s-1","monto":2000000}}'

# 3) consultar
curl -s http://localhost:8080/api/v1/reportes/solicitudes-aprobadas -H "Authorization: Bearer $TOKEN"
```

## Pruebas y calidad

```bash
./gradlew build jacocoMergedReport
```

Incluye pruebas unitarias, de integración con Testcontainers (LocalStack con SQS y DynamoDB reales) y la verificación de arquitectura. El dominio (`model` y `usecase`) debe cubrir al menos el 90 % de líneas y ramas.

## CI/CD

GitHub Actions (`.github/workflows/ci.yml`): `Lint` (hadolint, shellcheck, compose y estructura), `Calidad y build` (pruebas y cobertura) e `Imagen Docker`.
En `develop` y `main` la imagen se publica en Amazon ECR (`crediya/reportes:sha-<commit>`, etiquetas inmutables) asumiendo el rol `crediya-gha-reportes` por OIDC, sin claves estáticas.
