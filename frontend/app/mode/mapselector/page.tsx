import type { Metadata } from "next";
import MapSelector from "./MapSelector";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Escolha um mapa aleatório para sua próxima partida.",
};

export default function MapSelectorPage() {
  return <MapSelector />;
}
