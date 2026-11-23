import { useQuery } from "@tanstack/react-query";
import { Cargando, EstadoError } from "../componentes/UI";
import type { ReactNode } from "react";
import { ApiError } from "../lib/http";
export function Remote<T>({
  keyName,
  fetcher,
  notFound,
  loading,
  children,
}: {
  keyName: readonly unknown[];
  fetcher: () => Promise<T>;
  notFound?: ReactNode;
  loading?: ReactNode;
  children: (data: T) => ReactNode;
}) {
  const query = useQuery({
    queryKey: keyName,
    queryFn: fetcher,
    ...(notFound !== undefined && {
      retry: (attempt: number, error: Error) =>
        attempt < 1 && !(error instanceof ApiError && error.status === 404),
    }),
  });
  if (query.isPending) return <>{loading ?? <Cargando />}</>;
  if (notFound !== undefined && query.error instanceof ApiError && query.error.status === 404)
    return <>{notFound}</>;
  if (query.isError)
    return (
      <EstadoError
        error={query.error}
        onReintentar={() => void query.refetch()}
      />
    );
  return <>{children(query.data)}</>;
}
export const field =
  "w-full rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-3";
export function Alert({ error }: { error: unknown }) {
  return error ? (
    <p role="alert" className="rounded-lg bg-red-50 text-red-700 p-3 my-3">
      {error instanceof Error ? error.message : String(error)}
    </p>
  ) : null;
}
