# PulsePass - Capa de persistencia

MVP academico de eventos, artistas y entradas. Este modulo implementa **solo la capa de persistencia**:
modelo relacional versionado con Flyway, entidades JPA, repositories Spring Data y pruebas de
integracion contra PostgreSQL real (Testcontainers).

**Stack:** Java 21, Spring Boot 4.1, Spring Data JPA / Hibernate, PostgreSQL, Flyway, Testcontainers, Maven.

## Modelo

```
Venue 1 --- N Event
Event N --- M Artist        (tabla event_artists, PK compuesta)
User  1 --- 1 UserProfile   (user_profiles.user_id UNIQUE)
User  1 --- N Ticket
Event 1 --- N Ticket
```

`Ticket` es una entidad (no un `@ManyToMany`) porque tiene datos propios: `ticketCode`, `type`, `price`,
`status` y `purchaseDate` (BR-006). Los enums (`EventCategory`, `EventStatus`, `TicketType`, `TicketStatus`)
se guardan con `@Enumerated(EnumType.STRING)` y ademas tienen `CHECK` en la base. El precio es
`BigDecimal` / `NUMERIC(12,2)`. Las entidades no usan Lombok `@Data`.

Paquetes: `com.pulse.PulsePass.model` (entidades), `model.enums`, `repository`.

## Migraciones (Flyway)

Flyway es el unico responsable del esquema; Hibernate corre con `ddl-auto=validate`.

| Migracion | Contenido |
|-----------|-----------|
| `V1__create_schema.sql` | 7 tablas con PK, FK, UNIQUE, CHECK e indices |
| `V2__insert_initial_artists.sql` | Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse |
| `V3__add_streaming_url_to_event.sql` | `events.streaming_url VARCHAR(500)` nullable |

Nunca se edita una migracion ya aplicada: todo cambio estructural va en una nueva `V<n>__*.sql`.

## Consultas

| Necesidad | Repository.metodo | Mecanismo |
|-----------|-------------------|-----------|
| Evento por codigo (con venue) | `EventRepository.findByEventCode` | Query Method + `@EntityGraph` |
| Cartelera publicada por fecha | `findByStatusOrderByEventDateAsc` | Query Method |
| Eventos de un venue por codigo | `findByVenue_Code` | Query Method (navega relacion) |
| Eventos por artista | `findByArtistStageName` | JPQL + JOIN + DISTINCT |
| Eventos por ciudad y artista | `findByCityAndArtistStageName` | JPQL, varias asociaciones |
| Eventos recomendados | `findRecommendedEvents` | JPQL, filtros + DISTINCT + orden |
| Usuario por email (sin mayusculas) | `UserRepository.findByEmailIgnoreCase` | Query Method |
| Tickets por email y estado | `TicketRepository.findByUser_EmailAndStatus` | Query Method |
| Tickets PAID por eventCode | `findByEvent_EventCodeAndStatus` | Query Method (filtro simple) |
| Conteo de tickets por estado | `countByEventCodeAndStatus` | JPQL + COUNT |
| Tickets de eventos futuros | `findTicketsOfEventsAfter` | JPQL, orden cronologico |

## Pruebas

Las pruebas usan un PostgreSQL real levantado con Testcontainers (sin H2), Flyway construye el esquema
y Hibernate lo valida. **Requiere Docker en ejecucion.**

```bash
./mvnw clean test
```
```powershell
.\mvnw.cmd spring-boot:run
```

| Clase | Cubre |
|-------|-------|
| `FlywayMigrationIT` | QT-001/002: V1, V2 y V3 desde una base vacia |
| `VenuePersistenceTest` | FR-VEN, AC-001, UNIQUE y CHECK |
| `EventRepositoryIT` | FR-EVT, FR-VEN-004, AC-002, AC-006 |
| `EventArtistIT` | FR-ART, N:M, AC-003, AC-007 |
| `UserProfileIT` | FR-USR, 1:1, AC-004 |
| `TicketRepositoryIT` | FR-TKT, AC-005, AC-008 |
| `EventSearchIT` | FR-SRC-002/003/004 |

Surefire esta configurado para incluir tambien las clases `*IT`, de modo que `mvn clean test` las ejecuta.

## Configuracion local

`application.properties` toma la conexion de las variables `DB_URL`, `DB_USER` y `DB_PASSWORD`
(por defecto `jdbc:postgresql://localhost:5432/pulsepass`, `postgres` / `postgres`). Las pruebas no
necesitan ninguna base instalada.

## API REST (Fase 3 - Controladores)

Base: `/api`. Todos los errores usan `ErrorResponse` (`timestamp`, `status`, `error`, `message`, `details`).

| Metodo | Endpoint | Service | Exito |
|--------|----------|---------|-------|
| GET | `/api/venues/{code}` | `VenueService.findByCode` | 200 |
| GET | `/api/venues/active` | `VenueService.findActiveVenues` | 200 |
| POST | `/api/events` | `EventService.create` | 201 |
| GET | `/api/events/{eventCode}` | `EventService.findByCode` | 200 |
| GET | `/api/events/published` | `EventService.findPublishedEvents` | 200 |
| PATCH | `/api/events/{eventCode}/publish` | `EventService.publish` | 200 |
| POST | `/api/events/{eventCode}/artists/{artistId}` | `EventService.addArtist` | 200 |
| GET | `/api/events/by-artist?stageName=` | `EventService.findByArtist` | 200 |
| GET | `/api/artists/{id}` | `ArtistService.findById` | 200 |
| GET | `/api/artists/by-stage-name?stageName=` | `ArtistService.findByStageName` | 200 |
| GET | `/api/artists/active` | `ArtistService.findActiveArtists` | 200 |
| POST | `/api/users` | `UserService.register` | 201 |
| GET | `/api/users/by-email?email=` | `UserService.findByEmail` | 200 |
| GET | `/api/users/by-username?username=` | `UserService.findByUsername` | 200 |
| POST | `/api/tickets` | `TicketService.purchase` | 201 |
| GET | `/api/tickets/{ticketCode}` | `TicketService.findByCode` | 200 |
| GET | `/api/tickets/by-user?email=` | `TicketService.findByUserEmail` | 200 |
| GET | `/api/events/{eventCode}/tickets/paid` | `TicketService.findPaidTicketsByEvent` | 200 |
| PATCH | `/api/tickets/{ticketCode}/cancel` | `TicketService.cancel` | 200 |
| PATCH | `/api/tickets/{ticketCode}/use` | `TicketService.markAsUsed` | 200 |

Errores: `400` validacion o JSON mal formado, `404` recurso inexistente, `409` duplicado o regla de negocio, `500` error inesperado.

### Swagger UI

Con la aplicacion en ejecucion, abre [Swagger UI](http://localhost:8080/swagger-ui.html)
para explorar y probar los endpoints. La especificacion OpenAPI esta disponible
en [JSON](http://localhost:8080/v3/api-docs) y [YAML](http://localhost:8080/v3/api-docs.yaml).

Para probar un endpoint protegido, ejecuta `POST /api/auth/login` desde Swagger UI,
copia el campo `accessToken` y pegalo en **Authorize** sin el prefijo `Bearer`.
Usa una cuenta con el rol requerido por la operacion.

### Pruebas de Controller

Los tests `*ControllerTest` usan `@WebMvcTest`, `MockMvc` y `@MockitoBean` (sin PostgreSQL ni contexto completo):

```bash
./mvnw test -Dtest='*ControllerTest'
```

# PulsePass — Capa de seguridad (feature/security)

## Preparación

- Java 21, PostgreSQL y Maven (o `bash mvnw`).
- Establecer `PULSEPASS_JWT_SECRET_BASE64` con una clave aleatoria nueva de al menos 32 bytes: `openssl rand -base64 32`.
- Configurar `DB_URL`, `DB_USER`, `DB_PASSWORD` según su entorno.
- Ejecutar `bash mvnw clean test` y `bash mvnw spring-boot:run`.
- Flyway ejecuta `V4__security_credentials_roles.sql` sin modificar V1–V3.

## Flujo

1. `POST /api/users` con username, email, firstName, lastName, birthDate, password (mínimo 12 caracteres), y opcionalmente phone y city.
2. `POST /api/auth/login` con `identifier` (username o email) y `password`.
3. Enviar `Authorization: Bearer <accessToken>` para las rutas protegidas.
4. `GET /api/auth/me` devuelve el ID, usuario, correo y roles actuales.

Las cuentas preexistentes no tienen contraseña asignada y no pueden iniciar sesión hasta que un administrador habilite credenciales mediante un proceso seguro. Los roles STAFF y ADMIN no se conceden en registro público; se asignan mediante una operación administrativa controlada en `user_roles`.

## Limitaciones del MVP

STAFF opera tickets globalmente, sin asignación a eventos; no hay refresh token, revocación inmediata ni rate limiting. Requiere HTTPS y un gestor de secretos en producción.

## Verificación de este entorno

No se pudo ejecutar Maven: el wrapper intentó descargar Maven 3.10.1 desde Maven Central y la descarga falló. Se requiere compilar y ejecutar las pruebas en una máquina con Maven/dependencias disponibles; **no se afirma compilación verificada**.
