import { z } from "zod";
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public details: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}
const errorSchema = z.object({
  message: z.string().optional(),
  details: z.record(z.string(), z.string()).optional(),
});
let tokenGetter: () => string | null = () => null;
let unauthorized: () => void = () => {};
export function setHttpAuth(
  get: () => string | null,
  onUnauthorized: () => void,
) {
  tokenGetter = get;
  unauthorized = onUnauthorized;
}
export async function http<T>(
  path: string,
  schema: z.ZodType<T>,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set("Accept", "application/json");
  if (options.body) headers.set("Content-Type", "application/json");
  const token = tokenGetter();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(path, { ...options, headers });
  } catch {
    throw new ApiError(
      0,
      "No se pudo conectar con el servidor. Comprueba que Spring Boot esté iniciado.",
    );
  }
  if (!response.ok) {
    const raw: unknown = await response.json().catch(() => null);
    const parsed = errorSchema.safeParse(raw);
    if (response.status === 401 && token) unauthorized();
    const messages: Record<number, string> = {
      400: "Revisa los datos enviados.",
      401: "Credenciales inválidas o sesión expirada.",
      403: "No tienes permisos para esta operación.",
      404: "Recurso no encontrado.",
      409: "La operación no puede completarse por una regla de negocio.",
      500: "Error interno del servidor.",
    };
    throw new ApiError(
      response.status,
      parsed.success && parsed.data.message
        ? parsed.data.message
        : (messages[response.status] ?? `Error HTTP ${response.status}`),
      parsed.success ? (parsed.data.details ?? {}) : {},
    );
  }
  const raw: unknown = await response.json();
  return schema.parse(raw);
}
export const json = (body: unknown): RequestInit => ({
  method: "POST",
  body: JSON.stringify(body),
});
export const patch: RequestInit = { method: "PATCH" };
