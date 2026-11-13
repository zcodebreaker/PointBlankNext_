"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  getNotificationStorageKey,
  NOTIFICATION_DISMISSALS_CHANGED,
  readDismissedNotifications,
} from "@/lib/notification-dismissals";

type NotificationBadgeProps = {
  notificationIds: string[];
  playerId: number | null;
};

export default function NotificationBadge({ notificationIds, playerId }: NotificationBadgeProps) {
  const notificationSignature = notificationIds.join("\u001f");
  const [visibleCount, setVisibleCount] = useState<number | null>(null);

  useEffect(() => {
    if (playerId === null) {
      setVisibleCount(0);
      return;
    }

    const currentNotificationIds = notificationSignature ? notificationSignature.split("\u001f") : [];
    const storageKey = getNotificationStorageKey(playerId);
    const syncVisibleCount = () => {
      const dismissed = new Set(readDismissedNotifications(playerId));
      setVisibleCount(currentNotificationIds.filter((notificationId) => !dismissed.has(notificationId)).length);
    };
    const syncFromStorage = (event: StorageEvent) => {
      if (event.key === storageKey) syncVisibleCount();
    };

    syncVisibleCount();
    window.addEventListener(NOTIFICATION_DISMISSALS_CHANGED, syncVisibleCount);
    window.addEventListener("storage", syncFromStorage);

    return () => {
      window.removeEventListener(NOTIFICATION_DISMISSALS_CHANGED, syncVisibleCount);
      window.removeEventListener("storage", syncFromStorage);
    };
  }, [notificationSignature, playerId]);

  if (visibleCount === null || visibleCount === 0) return null;

  return (
    <span className="noti">
      <Link
        href="/mypage/notifications"
        aria-label={`${visibleCount} ${visibleCount === 1 ? "notificação" : "notificações"}`}
      >
        <Image
          src="/images/common/icon_noti.gif"
          alt=""
          width={14}
          height={13}
          unoptimized
        />
        <span className="notification-count">{visibleCount}</span>
      </Link>
    </span>
  );
}
