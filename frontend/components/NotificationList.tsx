"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import {
  readDismissedNotifications,
  writeDismissedNotifications,
} from "@/lib/notification-dismissals";

export type AccountNotification = {
  id: string;
  ticketId: number;
  relativeDate: string;
};

type NotificationListProps = {
  notifications: AccountNotification[];
  playerId: number;
};

export default function NotificationList({ notifications, playerId }: NotificationListProps) {
  const [dismissed, setDismissed] = useState<string[]>([]);

  useEffect(() => {
    setDismissed(readDismissedNotifications(playerId));
  }, [playerId]);

  const visibleNotifications = notifications.filter((notification) => !dismissed.includes(notification.id));

  const dismissNotification = (notificationId: string) => {
    if (!window.confirm("Tem certeza de que deseja remover?")) {
      return;
    }

    const next = dismissed.includes(notificationId)
      ? dismissed
      : [...dismissed, notificationId];

    setDismissed(next);
    writeDismissedNotifications(playerId, next);
  };

  return (
    <ul className="list">
      {visibleNotifications.length === 0 ? (
        <div className="nodata_n">
          <Image
            src="/images/common/mypage/icon_nodata.png"
            alt=""
            width={27}
            height={34}
            unoptimized
          />
          <span>sem dados</span>
        </div>
      ) : (
        visibleNotifications.map((notification) => (
          <li key={notification.id}>
            <div className="date">{notification.relativeDate}</div>
            <div className="notice">
              <span className="label">notice</span>
              <Link href={`/ticket/${notification.ticketId}`} className="txt">
                <p>The answer is registered in the ticket you inquired.</p>
              </Link>
            </div>
            <button
              type="button"
              className="btn_close notification-close-button"
              aria-label="Remover notificação"
              onClick={() => dismissNotification(notification.id)}
            />
          </li>
        ))
      )}
    </ul>
  );
}
