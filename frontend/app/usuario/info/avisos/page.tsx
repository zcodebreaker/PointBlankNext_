import { permanentRedirect } from "next/navigation";

export default function LegacyAccountNotificationsPage() {
  permanentRedirect("/mypage/notifications");
}
