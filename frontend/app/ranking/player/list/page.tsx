import { permanentRedirect } from "next/navigation";

export default function LegacyPlayerRankingPage() {
  permanentRedirect("/ranking/individual/list");
}
