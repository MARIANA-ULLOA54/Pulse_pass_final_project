import { Link } from "react-router";
import clsx from "clsx";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import {
  CalendarDays,
  MapPin,
  Music,
  ShieldCheck,
  Ticket as TicketIcon,
  Star,
  GraduationCap,
  Clapperboard,
  Activity,
} from "lucide-react";
import type {
  EventSummary,
  EventDetail,
  Ticket,
  Category,
} from "../lib/schemas";
import { RUTAS } from "../lib/rutas";
const variants = {
  primario: "bg-marca-600 hover:bg-marca-700 text-white",
  secundario:
    "border border-slate-300 dark:border-slate-600 hover:bg-slate-100 dark:hover:bg-slate-800",
  peligro: "bg-red-600 hover:bg-red-700 text-white",
  fantasma: "hover:bg-slate-100 dark:hover:bg-slate-800",
};
export function Boton({
  variante = "primario",
  cargando = false,
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: keyof typeof variants;
  cargando?: boolean;
}) {
  return (
    <button
      {...props}
      disabled={cargando || props.disabled}
      className={clsx(
        "rounded-lg px-5 py-3 font-semibold transition disabled:opacity-50 min-h-11",
        variants[variante],
        className,
      )}
    >
      {cargando ? "Cargando…" : props.children}
    </button>
  );
}
export function Panel({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <section
      className={clsx(
        "rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm p-5",
        className,
      )}
    >
      {children}
    </section>
  );
}
export function Cargando() {
  return (
    <p role="status" className="py-10 text-center text-slate-500">
      Cargando información…
    </p>
  );
}
export function EstadoVacio({ mensaje }: { mensaje: string }) {
  return (
    <Panel>
      <p className="text-slate-500">{mensaje}</p>
    </Panel>
  );
}
export function EstadoError({
  error,
  onReintentar,
}: {
  error: unknown;
  onReintentar?: () => void;
}) {
  return (
    <Panel>
      <p role="alert" className="text-red-600">
        {error instanceof Error
          ? error.message
          : "No se pudo cargar la información."}
      </p>
      {onReintentar && (
        <Boton onClick={onReintentar} className="mt-3">
          Reintentar
        </Boton>
      )}
    </Panel>
  );
}
export const precio = (n: number) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    maximumFractionDigits: 0,
  }).format(n);
export const fecha = (d: string) =>
  new Date(d).toLocaleString("es-CO", {
    dateStyle: "medium",
    timeStyle: "short",
  });
const palette: Record<Category, { color: string; icon: typeof Music }> = {
  MUSIC: {
    color: "bg-indigo-100 text-indigo-700 dark:bg-indigo-950",
    icon: Music,
  },
  SPORTS: {
    color: "bg-green-100 text-green-700 dark:bg-green-950",
    icon: Activity,
  },
  TECHNOLOGY: {
    color: "bg-cyan-100 text-cyan-700 dark:bg-cyan-950",
    icon: ShieldCheck,
  },
  EDUCATION: {
    color: "bg-amber-100 text-amber-700 dark:bg-amber-950",
    icon: GraduationCap,
  },
  CULTURE: { color: "bg-rose-100 text-rose-700 dark:bg-rose-950", icon: Star },
  ENTERTAINMENT: {
    color: "bg-violet-100 text-violet-700 dark:bg-violet-950",
    icon: Clapperboard,
  },
};
export function AvatarArtista({ nombre }: { nombre: string }) {
  return (
    <span aria-hidden="true" className="inline-flex size-12 shrink-0 items-center justify-center rounded-full bg-indigo-100 text-indigo-800 font-bold">
      {nombre.slice(0, 2).toLocaleUpperCase("es-CO")}
    </span>
  );
}
export function EventBanner({ category }: { category: Category }) {
  const { color, icon: Icon } = palette[category];
  return (
    <div aria-hidden="true" className={clsx("h-32 flex items-center justify-center", color)}>
      <Icon size={48} strokeWidth={1.5} />
    </div>
  );
}
export function EventCard({ event }: { event: EventSummary | EventDetail }) {
  return (
    <Link
      to={RUTAS.evento(event.eventCode)}
      className="block rounded-xl overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition"
    >
      <EventBanner category={event.category} />
      <div className="p-4 space-y-2">
        <p className="text-xs font-bold tracking-wider text-marca-600">
          {event.category}
        </p>
        <h3 className="font-titulo font-bold text-lg">{event.name}</h3>
        <p className="flex items-center gap-2 text-sm text-slate-500">
          <CalendarDays size={15} />
          {fecha(event.eventDate)}
        </p>
        <p className="flex items-center gap-2 text-sm text-slate-500">
          <MapPin size={15} />
          {event.venueName}
        </p>
        <Badge status={event.status} />
      </div>
    </Link>
  );
}
export function Badge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    PUBLISHED: "bg-emerald-100 text-emerald-800",
    PAID: "bg-emerald-100 text-emerald-800",
    USED: "bg-slate-200 text-slate-800",
    CANCELLED: "bg-red-100 text-red-800",
    SOLD_OUT: "bg-amber-100 text-amber-800",
    RESERVED: "bg-sky-100 text-sky-800",
    DRAFT: "bg-slate-200 text-slate-800",
    Activa: "bg-emerald-100 text-emerald-800",
  };
  return (
    <span
      className={clsx(
        "inline-block rounded-full px-2.5 py-1 text-xs font-semibold",
        styles[status] ?? "bg-slate-200 text-slate-800",
      )}
    >
      {status}
    </span>
  );
}
export function TicketCard({ ticket }: { ticket: Ticket }) {
  return (
    <Link to={RUTAS.ticket(ticket.ticketCode)} className="block">
      <Panel className="hover:border-marca-500">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="flex items-center gap-2 font-bold">
              <TicketIcon size={18} />
              {ticket.eventName}
            </p>
            <p className="font-mono text-sm text-slate-500 mt-2">
              {ticket.ticketCode}
            </p>
            <p className="mt-2">
              {ticket.type} · {precio(ticket.price)}
            </p>
          </div>
          <Badge status={ticket.status} />
        </div>
      </Panel>
    </Link>
  );
}
export function Heading({
  title,
  subtitle,
}: {
  title: string;
  subtitle?: string;
}) {
  return (
    <div className="mb-7">
      <h1 className="font-titulo text-3xl font-extrabold tracking-tight">
        {title}
      </h1>
      {subtitle && <p className="text-slate-500 mt-2">{subtitle}</p>}
    </div>
  );
}
