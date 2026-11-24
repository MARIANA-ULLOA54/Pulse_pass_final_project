import { transferableAbortController } from "node:util";
import { StrictMode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { RutaPorRol, RutaProtegida, SoloAnonimo } from "../componentes/Guardas";
import Layout from "../componentes/Layout";
import { AuthProvider } from "../context/AuthContext";
import Cuenta from "../pages/Cuenta";
import Login from "../pages/Login";
import Registro from "../pages/Registro";

const persistentStorageWrite = vi.fn();
const storageKey = "pulsepass_session";
const identity = {
  userId: 1,
  username: "andrea",
  email: "andrea@example.com",
  roles: ["USER"],
};
const profile = {
  ...identity,
  id: 1,
  firstName: "Andrea",
  lastName: "Ruiz",
  birthDate: "1998-04-12",
  active: true,
};
const server = setupServer(
  http.get("*/api/auth/me", () => HttpResponse.json(identity)),
  http.get("*/api/users/by-email", () => HttpResponse.json(profile)),
);

beforeAll(() => {
  // Node fetch y React Router deben compartir la misma implementación de AbortSignal.
  const controller = transferableAbortController();
  vi.stubGlobal("AbortController", controller.constructor);
  vi.stubGlobal("AbortSignal", controller.signal.constructor);
  vi.stubGlobal("localStorage", { setItem: persistentStorageWrite });
  server.listen({ onUnhandledRequest: "error" });
});
afterEach(() => {
  cleanup();
  sessionStorage.clear();
  persistentStorageWrite.mockClear();
  vi.restoreAllMocks();
  server.resetHandlers();
});
afterAll(() => {
  server.close();
  vi.unstubAllGlobals();
});

function open(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter([{
    element: <AuthProvider><Layout /></AuthProvider>,
    children: [
      { path: "/", element: <h1>Inicio</h1> },
      { path: "/403", element: <h1>Sin permisos</h1> },
      {
        element: <SoloAnonimo />,
        children: [
          { path: "/login", element: <Login /> },
          { path: "/registro", element: <Registro /> },
        ],
      },
      {
        element: <RutaProtegida />,
        children: [
          { path: "/cuenta", element: <Cuenta /> },
          {
            element: <RutaPorRol roles={["ADMIN"]} />,
            children: [{ path: "/admin/eventos", element: <h1>Administración</h1> }],
          },
        ],
      },
    ],
  }], { initialEntries: [path] });

  render(
    <StrictMode>
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>
    </StrictMode>,
  );
  return { client, router };
}

function change(label: string | RegExp, value: string) {
  fireEvent.change(screen.getByLabelText(label), { target: { value } });
}

function fillLogin() {
  change("Usuario o correo", "andrea");
  change("Contraseña", "Clave de prueba");
  fireEvent.click(screen.getByRole("button", { name: "Ingresar" }));
}

function fillRegistration(password: string) {
  change("Nombre de usuario", "andrea");
  change("Correo electrónico", identity.email);
  change("Nombres", "Andrea");
  change("Apellidos", "Ruiz");
  change("Fecha de nacimiento", profile.birthDate);
  change(/^Contraseña/, password);
  fireEvent.click(screen.getByRole("button", { name: "Crear cuenta" }));
}

function saveSession(lifetime = 60_000) {
  sessionStorage.setItem(storageKey, JSON.stringify({
    token: "demo-token",
    expiresAt: Date.now() + lifetime,
  }));
}

describe("Autenticación", () => {
  it("carga la identidad con Bearer y vuelve a la URL protegida completa", async () => {
    const login = vi.fn(async (request: Request) => {
      expect(request.headers.get("Authorization")).toBeNull();
      expect(await request.json()).toEqual({ identifier: "andrea", password: "Clave de prueba" });
      return HttpResponse.json({ accessToken: "demo-token", tokenType: "Bearer", expiresIn: 900 });
    });
    const me = vi.fn((request: Request) => {
      expect(request.headers.get("Authorization")).toBe("Bearer demo-token");
      return HttpResponse.json(identity);
    });
    server.use(http.post("*/api/auth/login", ({ request }) => login(request)));
    server.use(http.get("*/api/auth/me", ({ request }) => me(request)));

    const { router } = open("/cuenta?origen=evento#perfil");
    await screen.findByRole("heading", { name: "Iniciar sesión" });
    expect(router.state.location.state).toEqual({ desde: "/cuenta?origen=evento#perfil" });
    fillLogin();

    expect(await screen.findByText("Andrea Ruiz")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Mi cuenta (andrea)" })).toBeInTheDocument();
    expect(router.state.location).toMatchObject({ pathname: "/cuenta", search: "?origen=evento", hash: "#perfil" });
    expect(login).toHaveBeenCalledTimes(1);
    expect(me).toHaveBeenCalledTimes(1);
    expect(sessionStorage.getItem(storageKey)).toContain("demo-token");
    expect(persistentStorageWrite).not.toHaveBeenCalled();
  });

  it("presenta un mensaje genérico si el login responde 401", async () => {
    server.use(http.post("*/api/auth/login", () =>
      HttpResponse.json({ message: "Usuario inexistente" }, { status: 401 }),
    ));
    open("/login");
    await screen.findByRole("button", { name: "Ingresar" });
    fillLogin();
    expect(await screen.findByRole("alert")).toHaveTextContent("Credenciales inválidas");
    expect(screen.queryByText("Usuario inexistente")).toBeNull();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });

  it("valida los campos vacíos del login antes de enviar el formulario", async () => {
    const request = vi.fn(() => HttpResponse.json({}));
    server.use(http.post("*/api/auth/login", request));
    open("/login");
    fireEvent.click(await screen.findByRole("button", { name: "Ingresar" }));
    expect(await screen.findByText("Ingresa tu usuario o correo")).toBeInTheDocument();
    expect(screen.getByText("Ingresa tu contraseña")).toBeInTheDocument();
    expect(request).not.toHaveBeenCalled();
  });

  it("valida la longitud de la contraseña de registro antes de enviar", async () => {
    const request = vi.fn(() => HttpResponse.json(profile));
    server.use(http.post("*/api/users", request));
    open("/registro");
    await screen.findByRole("button", { name: "Crear cuenta" });
    fillRegistration("corta");
    expect(await screen.findByText("Usa al menos 12 caracteres")).toBeInTheDocument();
    expect(request).not.toHaveBeenCalled();
  });

  it("confirma el registro y redirige a login después de un 201", async () => {
    server.use(http.post("*/api/users", async ({ request }) => {
      expect(await request.json()).toEqual({
        username: "andrea", email: identity.email, firstName: "Andrea",
        lastName: "Ruiz", birthDate: profile.birthDate, password: "Clave de prueba",
      });
      return HttpResponse.json(profile, { status: 201 });
    }));
    const { router } = open("/registro");
    await screen.findByRole("button", { name: "Crear cuenta" });
    fillRegistration("Clave de prueba");
    expect(await screen.findByText("Cuenta creada, inicia sesión.")).toBeInTheDocument();
    expect(router.state.location.pathname).toBe("/login");
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });

  it("muestra un mensaje genérico si el registro responde 409", async () => {
    server.use(http.post("*/api/users", () =>
      HttpResponse.json({ message: "Conflicto interno" }, { status: 409 }),
    ));
    open("/registro");
    await screen.findByRole("button", { name: "Crear cuenta" });
    fillRegistration("Clave de prueba");
    expect(await screen.findByRole("alert")).toHaveTextContent("El usuario o correo ya está registrado");
    expect(screen.queryByText("Conflicto interno")).toBeNull();
  });

  it("asocia los detalles de un 400 con sus campos", async () => {
    server.use(http.post("*/api/users", () => HttpResponse.json({
      details: { email: "Correo rechazado", other: "Detalle interno" },
    }, { status: 400 })));
    open("/registro");
    await screen.findByRole("button", { name: "Crear cuenta" });
    fillRegistration("Clave de prueba");
    expect(await screen.findByLabelText("Correo electrónico")).toHaveAccessibleDescription("Correo rechazado");
    expect(screen.getByRole("alert")).toHaveTextContent("Revisa los datos enviados.");
  });

  it.each([
    ["USER", "Sin permisos"],
    ["STAFF", "Sin permisos"],
    ["ADMIN", "Administración"],
  ])("restaura una sesión %s y aplica la guarda de administración", async (role, heading) => {
    saveSession();
    server.use(http.get("*/api/auth/me", () => HttpResponse.json({ ...identity, roles: [role] })));
    open("/admin/eventos");
    expect(await screen.findByRole("heading", { name: heading })).toBeInTheDocument();
  });

  it("consulta el perfil por el nombre de usuario de la sesión", async () => {
    saveSession();
    const lookup = vi.fn((request: Request) => {
      expect(new URL(request.url).searchParams.get("username")).toBe("andrea");
      return HttpResponse.json(profile);
    });
    server.use(http.get("*/api/users/by-username", ({ request }) => lookup(request)));
    open("/cuenta");
    await screen.findByText("Andrea Ruiz");
    fireEvent.click(screen.getByRole("button", { name: "Consultar por usuario" }));
    expect(await screen.findByText("Andrea Ruiz")).toBeInTheDocument();
    expect(lookup).toHaveBeenCalledTimes(1);
  });

  it("borra el token y la caché al cerrar sesión", async () => {
    saveSession();
    const { client, router } = open("/cuenta");
    expect(await screen.findByText("Andrea Ruiz")).toBeInTheDocument();
    expect(client.getQueryData(["profile", "email", identity.email])).toEqual(expect.objectContaining({ id: 1 }));
    fireEvent.click(screen.getByRole("button", { name: "Cerrar sesión" }));
    expect(await screen.findByRole("heading", { name: "Inicio" })).toBeInTheDocument();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    expect(client.getQueryCache().getAll()).toHaveLength(0);
    await act(() => router.navigate("/cuenta"));
    expect(await screen.findByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
  });

  it("descarta una sesión almacenada que ya venció", async () => {
    saveSession(-1);
    const me = vi.fn(() => HttpResponse.json(identity));
    server.use(http.get("*/api/auth/me", me));
    open("/cuenta");
    expect(await screen.findByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    expect(me).not.toHaveBeenCalled();
  });

  it("limpia la sesión y conserva el destino si una petición autenticada responde 401", async () => {
    saveSession();
    server.use(http.get("*/api/users/by-email", () => HttpResponse.json({}, { status: 401 })));
    const { client, router } = open("/cuenta?origen=evento");
    expect(await screen.findByText(/Tu sesión expiró/)).toBeInTheDocument();
    expect(router.state.location.state).toEqual({ motivo: "expirada", desde: "/cuenta?origen=evento" });
    expect(sessionStorage.getItem(storageKey)).toBeNull();
    expect(client.getQueryCache().getAll()).toHaveLength(0);
  });

  it("cierra la sesión al alcanzar su fecha de expiración sin otra petición", async () => {
    const timers = vi.spyOn(window, "setTimeout");
    saveSession();
    open("/cuenta");
    await screen.findByText("Andrea Ruiz");
    const expiration = timers.mock.calls.find(([, delay]) => delay && delay > 50_000 && delay <= 60_000)?.[0];
    expect(expiration).toBeTypeOf("function");
    if (typeof expiration === "function") act(() => expiration());
    expect(await screen.findByText(/Tu sesión expiró/)).toBeInTheDocument();
    expect(sessionStorage.getItem(storageKey)).toBeNull();
  });
});
