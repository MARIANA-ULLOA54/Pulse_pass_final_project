import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "../context/AuthContext";
import CatalogoEventos from "../pages/CatalogoEventos";
import DetalleArtista from "../pages/DetalleArtista";
import DetalleEvento from "../pages/DetalleEvento";
import DetalleSede from "../pages/DetalleSede";
import ListaArtistas from "../pages/ListaArtistas";
import ListaSedes from "../pages/ListaSedes";

const artista = { id: 1, stageName: "Solar & Beat", genre: "Electrónica", active: true };
const sede = { id: 1, code: "VEN-01", name: "Marina", city: "Santa Marta", capacity: 30, active: true };
const evento = {
  id: 1, eventCode: "FEST", name: "Festival del Caribe", category: "MUSIC",
  status: "PUBLISHED", eventDate: "2027-12-12T20:00:00", venueCode: sede.code,
  venueName: sede.name, minimumAge: 18, artists: [artista],
};
const porArtista = "/api/events/by-artist?stageName=Solar%20%26%20Beat";

function URLActual() {
  const { search } = useLocation();
  return <span data-testid="query">{search}</span>;
}

function abrir(path: string, responses: Record<string, unknown> = {}) {
  const fetchMock = vi.fn(async (url: string) =>
    Object.hasOwn(responses, url)
      ? Response.json(responses[url])
      : Response.json({ message: "Recurso no encontrado" }, { status: 404 }),
  );
  vi.stubGlobal("fetch", fetchMock);
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]}>
        <AuthProvider>
          <URLActual />
          <Routes>
            <Route path="/eventos" element={<CatalogoEventos />} />
            <Route path="/eventos/:eventCode" element={<DetalleEvento />} />
            <Route path="/artistas" element={<ListaArtistas />} />
            <Route path="/artistas/:id" element={<DetalleArtista />} />
            <Route path="/sedes" element={<ListaSedes />} />
            <Route path="/sedes/:code" element={<DetalleSede />} />
            <Route path="/login" element={<h1>Iniciar sesión</h1>} />
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
  return fetchMock;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("Catálogo público", () => {
  it("muestra eventos publicados y actualiza los filtros en la URL", async () => {
    const fetchMock = abrir("/eventos", {
      "/api/events/published": [evento],
      "/api/artists/active": [artista],
      [porArtista]: [evento],
    });

    expect(await screen.findByRole("link", { name: /Festival del Caribe/ })).toHaveAttribute("href", "/eventos/FEST");
    await screen.findByRole("option", { name: artista.stageName });
    fireEvent.change(screen.getByLabelText("Artista"), { target: { value: artista.stageName } });
    await waitFor(() => expect(fetchMock).toHaveBeenCalledWith(porArtista, expect.anything()));
    fireEvent.change(screen.getByLabelText("Categoría"), { target: { value: "MUSIC" } });
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "Otro" } });
    expect(await screen.findByText("Sin resultados para tu búsqueda")).toBeInTheDocument();
    const params = new URLSearchParams(screen.getByTestId("query").textContent ?? "");
    expect(Object.fromEntries(params)).toEqual({ q: "Otro", categoria: "MUSIC", artista: artista.stageName });

    fireEvent.click(screen.getByRole("button", { name: "Limpiar" }));
    expect(await screen.findByText(evento.name)).toBeInTheDocument();
    expect(screen.getByTestId("query")).toBeEmptyDOMElement();
  });

  it("muestra el estado vacío de la cartelera", async () => {
    abrir("/eventos", { "/api/events/published": [], "/api/artists/active": [] });
    expect(await screen.findByText("Aún no hay eventos publicados")).toBeInTheDocument();
  });

  it("muestra artistas, sede y desactiva la compra de un evento agotado", async () => {
    abrir("/eventos/FEST", {
      "/api/events/FEST": { ...evento, status: "SOLD_OUT" },
      "/api/venues/VEN-01": sede,
    });
    expect(await screen.findByRole("heading", { name: evento.name })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: artista.stageName })).toHaveAttribute("href", "/artistas/1");
    expect(screen.getByRole("link", { name: sede.name })).toHaveAttribute("href", "/sedes/VEN-01");
    expect(await screen.findByText(/Santa Marta/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Comprar entrada" })).toBeDisabled();
  });

  it("redirige a login a visitantes que quieren comprar", async () => {
    abrir("/eventos/FEST", { "/api/events/FEST": evento, "/api/venues/VEN-01": sede });
    fireEvent.click(await screen.findByRole("button", { name: "Inicia sesión para comprar" }));
    expect(await screen.findByRole("heading", { name: "Iniciar sesión" })).toBeInTheDocument();
  });

  it("muestra el 404 de un evento", async () => {
    abrir("/eventos/inexistente");
    expect(await screen.findByRole("heading", { name: "Evento no disponible" })).toBeInTheDocument();
  });

  it("lista artistas y resuelve el perfil por nombre con sus eventos", async () => {
    abrir("/artistas", {
      "/api/artists/active": [artista],
      "/api/artists/by-stage-name?stageName=Solar%20%26%20Beat": artista,
      "/api/artists/1": artista,
      [porArtista]: [evento],
    });
    expect(await screen.findByRole("link", { name: /Solar & Beat/ })).toBeInTheDocument();
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: artista.stageName } });
    fireEvent.click(screen.getByRole("button", { name: "Buscar" }));
    expect(await screen.findByRole("heading", { name: artista.stageName, level: 1 })).toBeInTheDocument();
    expect(await screen.findByRole("link", { name: /Festival del Caribe/ })).toBeInTheDocument();
  });

  it("rechaza un ID no numérico sin llamar al backend", () => {
    const fetchMock = abrir("/artistas/invalido");
    expect(screen.getByRole("heading", { name: /Página no encontrada/ })).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("navega de la lista de sedes al detalle", async () => {
    abrir("/sedes", { "/api/venues/active": [sede], "/api/venues/VEN-01": sede });
    fireEvent.click(await screen.findByRole("link", { name: /Marina/ }));
    expect(await screen.findByRole("heading", { name: sede.name })).toBeInTheDocument();
    expect(screen.getByText(sede.code)).toBeInTheDocument();
  });

  it.each(["/sedes/inexistente", "/artistas/100"])("muestra el 404 para %s", async (path) => {
    abrir(path);
    expect(await screen.findByRole("heading", { name: /Página no encontrada/ })).toBeInTheDocument();
  });
});
