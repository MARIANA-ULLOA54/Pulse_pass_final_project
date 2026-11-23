import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { Heading, Panel } from "../componentes/UI";
import { Remote } from "./shared";
export default function Cuenta() {
  const { session } = useAuth();
  if (session.status !== "authenticated") return null;
  return (
    <>
      <Heading title="Mi cuenta" subtitle="Información personal" />
      <Panel className="mb-5">
        <p>
          <strong>Usuario:</strong> {session.user.username}
        </p>
        <p>
          <strong>Correo:</strong> {session.user.email}
        </p>
        <p>
          <strong>Roles:</strong> {session.user.roles.join(", ")}
        </p>
      </Panel>
      <Remote
        keyName={["profile", session.user.email]}
        fetcher={() => api.userByEmail(session.user.email)}
      >
        {(u) => (
          <Panel>
            <p>
              <strong>Nombre:</strong> {u.firstName} {u.lastName}
            </p>
            <p>
              <strong>Ciudad:</strong> {u.city || "Sin registrar"}
            </p>
            <p>
              <strong>Fecha de nacimiento:</strong> {u.birthDate}
            </p>
          </Panel>
        )}
      </Remote>
    </>
  );
}
