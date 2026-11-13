import type { Metadata } from "next";
import ModeSelector from "./ModeSelector";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Escolha aleatoriamente um modo para sua próxima partida de Point Blank.",
};

export default function ModeSelectorPage() {
  return <ModeSelector />;
}
