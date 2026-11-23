import React from "react";
import { createRoot } from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RouterProvider, createBrowserRouter } from "react-router";
import Layout from "./componentes/Layout";
import { RutaProtegida, RutaPorRol, SoloAnonimo } from "./componentes/Guardas";
import { AuthProvider } from "./context/AuthContext";
import Inicio from "./pages/Inicio";
import "./index.css";
const pages = import.meta.glob<{ default: React.ComponentType }>(
  "./pages/*.tsx",
);
const lazy = (name: string) => async () => {
  const load = pages[`./pages/${name}.tsx`];
  if (!load) throw new Error("Página inexistente");
  return { Component: (await load()).default };
};
const router = createBrowserRouter([
  {
    path: "/",
    Component: Layout,
    children: [
      { index: true, Component: Inicio },
      { path: "eventos", lazy: lazy("CatalogoEventos") },
      { path: "eventos/:eventCode", lazy: lazy("DetalleEvento") },
      { path: "artistas", lazy: lazy("ListaArtistas") },
      { path: "artistas/:id", lazy: lazy("DetalleArtista") },
      { path: "sedes", lazy: lazy("ListaSedes") },
      { path: "sedes/:code", lazy: lazy("DetalleSede") },
      {
        Component: SoloAnonimo,
        children: [
          { path: "login", lazy: lazy("Login") },
          { path: "registro", lazy: lazy("Registro") },
        ],
      },
      {
        Component: RutaProtegida,
        children: [
          { path: "cuenta", lazy: lazy("Cuenta") },
          {
            element: <RutaPorRol roles={["USER"]} />,
            children: [
              { path: "mis-tickets", lazy: lazy("MisTickets") },
              { path: "mis-tickets/:ticketCode", lazy: lazy("DetalleTicket") },
              {
                path: "compra/confirmacion/:ticketCode",
                lazy: lazy("ConfirmacionCompra"),
              },
            ],
          },
          {
            element: <RutaPorRol roles={["STAFF", "ADMIN"]} />,
            children: [
              { path: "staff/validacion", lazy: lazy("ValidacionIngreso") },
              {
                path: "staff/eventos/:eventCode/tickets",
                lazy: lazy("TicketsPagadosEvento"),
              },
            ],
          },
          {
            element: <RutaPorRol roles={["ADMIN"]} />,
            children: [
              { path: "admin/eventos", lazy: lazy("AdminEventos") },
              { path: "admin/eventos/nuevo", lazy: lazy("AdminCrearEvento") },
              {
                path: "admin/eventos/:eventCode",
                lazy: lazy("AdminGestionEvento"),
              },
            ],
          },
        ],
      },
      { path: "403", lazy: lazy("SinPermisos") },
      { path: "*", lazy: lazy("NoEncontrada") },
    ],
  },
]);
const client = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 30000 } },
});
createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <QueryClientProvider client={client}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </React.StrictMode>,
);
