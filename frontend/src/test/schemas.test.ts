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
});
