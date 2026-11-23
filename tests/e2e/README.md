# PulsePass — Fase 5: Postman E2E manual

## Entrega
- `postman/PulsePass-Manual-E2E.postman_collection.json`: colección v2.1 con las 22 rutas nominales y escenarios negativos; sin scripts.
- `postman/PulsePass-local.postman_environment.json`: variables editables sin secretos.
- `fixtures/seed-e2e.sql`: fixture exclusivo E2E para venue y actores STAFF/ADMIN (contraseñas pedidas por psql, BCrypt con pgcrypto).
- `checklist-e2e-manual.md` y `registro-ejecucion-manual.md`.

## Arranque en PowerShell
1. Crear la base `pulsepass_e2e` en PostgreSQL si todavía no existe. Desde pgAdmin, conectarse a `postgres` y ejecutar `CREATE DATABASE pulsepass_e2e;`.
2. Desde la raíz del proyecto, ajustar las credenciales a las de PostgreSQL y arrancar el backend:
```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/pulsepass_e2e'
$env:DB_USER = 'postgres'
$env:DB_PASSWORD = 'postgres'
$jwtBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$jwtRandom.Dispose()
$env:PULSEPASS_JWT_SECRET_BASE64 = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```
3. Cuando el backend termine de arrancar, ejecutar desde otra terminal en la raíz del proyecto y responder las contraseñas de STAFF y ADMIN:
```powershell
psql -h localhost -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
```
Si `psql` no está en `PATH`, invocar su ejecutable desde la carpeta `bin` de PostgreSQL, por ejemplo `& 'C:\Program Files\PostgreSQL\17\bin\psql.exe'` con los mismos argumentos. El archivo usa `\prompt`, un comando propio de `psql`. En pgAdmin se puede ejecutar desde **PSQL Tool** con `\i 'C:/ruta/al/proyecto/tests/e2e/fixtures/seed-e2e.sql'`, con la sesión conectada a `pulsepass_e2e`.

## Postman paso a paso
1. Abrir Postman → Import → elegir ambos JSON de `tests/e2e/postman/`.
2. Seleccionar el environment `PulsePass Local E2E` (esquina superior derecha).
3. En variables, completar `userAPassword` y `userBPassword` con valores fuertes (mínimo 12 caracteres), y `staffPassword`/`adminPassword` con las elegidas en psql. Guardarlas como valores locales/secretos, nunca exportar credenciales.
4. Abrir `00 Preparación / conectividad` → GET venues activos → **Send**. Debe devolver 200 y contener `VEN-E2E`.
5. `01 Venues` y `02 Artists`: obtener el `id` real del artista Solar Beat y actualizar `artistId` si difiere de 1.
6. `03 Auth / usuarios`: enviar registros A y B (201), logins A/B/STAFF/ADMIN (200). De cada respuesta copiar SOLO `accessToken` a `userAToken`, `userBToken`, `staffToken`, `adminToken`. Ejecutar GET `/api/auth/me` y verificar roles/identidad. Los JWT duran 900 segundos; renovar si expiran.
7. `05 Events`: ADMIN crea evento (201). Usar un `newEventCode` nuevo en cada corrida, copiar código a `draftEventCode` y `eventToPublishCode`. Antes de publicar, probar borrador anónimo. Asociar artista; publicar; copiar código a `publishedEventCode`; comprobar consultas públicas.
8. `06 Tickets`: USER_A compra **dos tickets diferentes** (201) y copia códigos a `ticketCancelableCode` y `ticketUsableCode`. Antes de mutarlos, ejecutar negativos de USER_B y verificar que no cambian. Cancelar T1 con USER_A y usar T2 con STAFF; verificar estados con GET posterior.
9. `07 Seguridad` y `08 Validaciones`: enviar individualmente, comprobar códigos y estructura `ErrorResponse(timestamp,status,error,message,details)`. Donde se indique `403/404`, documentar la política real de anti-enumeración, sin aceptar cualquiera arbitrariamente.
10. Rellenar `registro-ejecucion-manual.md` con HTTP real, PASS/FAIL/BLOCKED y observaciones. **No usar Runner, scripts, pm.test ni Newman**.

## Advertencias
- El PRD requiere verificación manual real contra backend y PostgreSQL; el ZIP NO certifica pruebas ejecutadas.
- `TC-HTTP-JSON` requiere corromper manualmente el cuerpo antes de Send; el JSON inicial es válido para facilitar edición.
- `TC-SEC-016` usa usuario adicional único; comprobar que `role=ADMIN` no escala privilegios.
- Algunos casos negativos deben ejecutarse **antes** de cancelar o usar tickets.
- `2030-08-20T20:00:00` es una fecha futura fija de prueba; cambiar si la ejecución ocurre después de esa fecha.
- No se añaden endpoints ni cambios a la API: la colección respeta los DTO y enums existentes (`MUSIC`, `GENERAL`).
