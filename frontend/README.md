# PulsePass — Frontend Fase 5

Aplicación React + TypeScript + Tailwind CSS v4 para la API Spring Boot existente. Rama original: `feature/frontend-base`.

## Requisitos
- Node.js 22.22 o superior (o versión compatible con Vite 7)
- Backend Spring Boot en `http://localhost:8080` y PostgreSQL iniciado

## Ejecutar (Git Bash / terminal de IntelliJ)
```bash
cd frontend
npm install
npm run dev
```
Abrir http://localhost:5173. Vite reenvía `/api` a `localhost:8080`.

## Verificaciones
```bash
npm run lint
npm run typecheck
npm run build
npm test
```

## Funcionalidades
20 rutas según el PRD: inicio, catálogo, detalle de eventos, artistas, sedes, registro, login, cuenta, compra y cancelación de tickets, validación STAFF, y creación/publicación/asociación de artistas ADMIN.

En `feature/frontend-catalog`, la cartelera guarda sus filtros en `q`, `categoria` y `artista`. El filtro de artista usa el nombre artístico, tal como lo recibe la API. Por ejemplo, `/eventos?categoria=MUSIC&artista=Solar%20Beat`.

## Seguridad
Token JWT temporal en `sessionStorage`, cierre de sesión al expirar, guardas por rol. La autorización real siempre corresponde al backend. No se guarda la contraseña.

## Limitaciones del backend
No existe GET de todos los eventos para ADMIN: la vista permite buscar por código y recuerda códigos creados en esta pestaña. La compra no calcula precio ni procesa pagos.

Las pruebas de catálogo simulan las respuestas de la API. Para probar con datos reales, inicia Spring Boot y PostgreSQL según los requisitos de arriba.

## Autenticación

- `/registro` crea una cuenta con rol de comprador y lleva a `/login`.
- `/login` acepta usuario o correo. Después de iniciar sesión, el frontend consulta `/api/auth/me` y vuelve a la página que solicitó autenticación.
- `/cuenta` muestra el perfil propio y permite cerrar sesión. Las guardas y la navegación se ajustan a los roles `USER`, `STAFF` y `ADMIN` que devuelve la API.
- Cuando vence la sesión o una solicitud autenticada recibe un 401, la app limpia el token y la caché y solicita iniciar sesión de nuevo.

El conjunto `src/test/auth.test.tsx` verifica estos flujos con Vitest, Testing Library y MSW.
