import { permanentRedirect } from "next/navigation";

export default function LegacyAccountInformationPage() {
  permanentRedirect("/mypage/info");
}
