import { z } from "zod";

const codigosSchema = z.array(z.string().trim().min(1).max(30));
const storageKey = "pulsepass_admin_codes";

export function leerCodigosDeEventosCreados(): string[] {
  try {
    const stored: unknown = JSON.parse(sessionStorage.getItem(storageKey) ?? "[]");
    const parsed = codigosSchema.safeParse(stored);
    return parsed.success ? [...new Set(parsed.data)] : [];
  } catch {
    return [];
  }
}

export function recordarEventoCreado(code: string): string[] {
  const codes = [...new Set([...leerCodigosDeEventosCreados(), code])];
  try {
    sessionStorage.setItem(storageKey, JSON.stringify(codes));
  } catch {
    // La gestión del evento sigue disponible aunque el navegador bloquee el almacenamiento.
  }
  return codes;
}
