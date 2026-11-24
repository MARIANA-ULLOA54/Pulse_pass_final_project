import { describe, it, expect } from "vitest";
import { registerSchema, ticketType } from "../lib/schemas";
describe("Validación", () => {
  it("exige contraseñas de al menos 12 caracteres", () => {
    expect(
      registerSchema.safeParse({
        username: "testuser",
        email: "test@example.com",
        firstName: "Ana",
        lastName: "Pérez",
        birthDate: "2000-01-01",
        password: "123",
      }).success,
    ).toBe(false);
  });
  it("admite los cuatro tipos de entrada", () => {
    expect(ticketType.options).toHaveLength(4);
  });

  it("valida la fecha de nacimiento y el límite de la contraseña en bytes", () => {
    const registro = {
      username: "andrea",
      email: "andrea@example.com",
      firstName: "Andrea",
      lastName: "Ruiz",
      birthDate: "1998-04-12",
      password: "é".repeat(36),
    };
    expect(registerSchema.safeParse(registro).success).toBe(true);
    expect(registerSchema.safeParse({ ...registro, password: "é".repeat(37) }).success).toBe(false);
    expect(registerSchema.safeParse({ ...registro, birthDate: "9999-01-01" }).success).toBe(false);
  });
});
