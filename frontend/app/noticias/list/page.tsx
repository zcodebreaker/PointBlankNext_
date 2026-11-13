import { permanentRedirect } from "next/navigation";

export default function LegacyNewsListPage() {
  permanentRedirect("/news/list");
}
