export const RUTAS = {
  inicio: "/",
  eventos: "/eventos",
  evento: (code: string) => `/eventos/${encodeURIComponent(code)}`,
  artistas: "/artistas",
  artista: (id: string | number) => `/artistas/${id}`,
  sedes: "/sedes",
  sede: (code: string) => `/sedes/${encodeURIComponent(code)}`,
  login: "/login",
  registro: "/registro",
  cuenta: "/cuenta",
  tickets: "/mis-tickets",
  ticket: (code: string) => `/mis-tickets/${encodeURIComponent(code)}`,
  confirmacion: (code: string) =>
    `/compra/confirmacion/${encodeURIComponent(code)}`,
  validacion: "/staff/validacion",
  pagados: (code: string) =>
    `/staff/eventos/${encodeURIComponent(code)}/tickets`,
  admin: "/admin/eventos",
  nuevo: "/admin/eventos/nuevo",
  gestionar: (code: string) => `/admin/eventos/${encodeURIComponent(code)}`,
  sinPermisos: "/403",
} as const;
