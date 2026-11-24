# Checklist de ejecución E2E manual

- [ ] Backend activo en base pulsepass_e2e; Flyway V1–V4 aplicado
- [ ] Fixtures venue y artistas existentes
- [ ] STAFF y ADMIN provisionados con contraseña BCrypt fuera de registro público
- [ ] Postman: importar colección y ambiente, seleccionar PulsePass Local E2E
- [ ] Configurar contraseñas en valores locales/secretos; no exportarlas
- [ ] Flujo A: consultar venues, artistas y publicados
- [ ] Flujo B: registrar USER_A y USER_B, hacer login de 4 actores, copiar JWT
- [ ] Flujo C: crear, asociar, publicar y verificar evento
- [ ] Flujo D: comprar T1/T2, probar IDOR, cancelar T1, usar T2
- [ ] Flujo E: validar 400, 401, 403, 404 y 409
- [ ] Revisar ErrorResponse con timestamp/status/error/message/details
- [ ] Registrar cada petición como PASS/FAIL/BLOCKED/NOT RUN
- [ ] Revisar ausencia de credenciales/tokens en evidencia
- [ ] Confirmar cobertura nominal 22/22 y limpiar solo ambiente E2E
