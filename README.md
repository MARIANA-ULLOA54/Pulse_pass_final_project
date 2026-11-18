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
