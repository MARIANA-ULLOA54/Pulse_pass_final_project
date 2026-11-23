import { Link } from "react-router";
import { Heading, Panel } from "../componentes/UI";
export default function NoEncontrada() {
  return (
    <Panel>
      <Heading
        title="404 · Página no encontrada"
        subtitle="La ruta solicitada no existe."
      />
      <Link to="/" className="text-marca-600">
        Volver al inicio
      </Link>
    </Panel>
  );
}
