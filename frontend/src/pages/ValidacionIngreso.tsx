import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { api } from "../lib/api";
import { Badge, Boton, Heading, Panel } from "../componentes/UI";
import { Remote, field, Alert } from "./shared";
export default function ValidacionIngreso() {
  const [code, setCode] = useState("");
  const [search, setSearch] = useState("");
  const client = useQueryClient();
  const mutation = useMutation({
    mutationFn: () => api.use(search),
    onSuccess: () =>
      void client.invalidateQueries({ queryKey: ["ticket", search] }),
  });
  return (
    <>
      <Heading
        title="Validación de ingreso"
        subtitle="Verifica entradas y registra su uso"
      />
      <form
        className="flex gap-3 mb-6 max-w-xl"
        onSubmit={(e) => {
          e.preventDefault();
          setSearch(code.trim());
          mutation.reset();
        }}
      >
        <label className="flex-1">
          Código del ticket
          <input
            className={field}
            value={code}
            onChange={(e) => setCode(e.target.value)}
            required
          />
        </label>
        <Boton type="submit" className="self-end">
          Buscar
        </Boton>
      </form>
      {search && (
        <Remote keyName={["ticket", search]} fetcher={() => api.ticket(search)}>
          {(t) => (
            <Panel className="max-w-xl">
              <h2 className="font-bold">{t.eventName}</h2>
              <p className="font-mono my-2">{t.ticketCode}</p>
              <Badge status={t.status} />
              <Alert error={mutation.error} />
              {mutation.isSuccess && (
                <p role="status" className="text-green-700 my-3">
                  Ingreso registrado correctamente.
                </p>
              )}
              <div className="mt-4">
                <Boton
                  disabled={t.status !== "PAID"}
                  cargando={mutation.isPending}
                  onClick={() => mutation.mutate()}
                >
                  Marcar como usado
                </Boton>
              </div>
            </Panel>
          )}
        </Remote>
      )}
    </>
  );
}
