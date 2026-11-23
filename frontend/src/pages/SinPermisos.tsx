import { Link } from "react-router";
import { Heading, Panel } from "../componentes/UI";
export default function SinPermisos() {
  return (
    <Panel>
      <Heading
        title="403 · Sin permisos"
        subtitle="Tu cuenta no tiene acceso a esta sección."
      />
      <Link to="/" className="text-marca-600">
        Volver al inicio
      </Link>
    </Panel>
  );
}
