export const NOTIFICATION_DISMISSALS_CHANGED = "pb-notification-dismissals-changed";

const storageVersion = 1;

export function getNotificationStorageKey(playerId: number) {
  return `pb-dismissed-notifications-v${storageVersion}-${playerId}`;
}

export function readDismissedNotifications(playerId: number) {
  try {
    const value = localStorage.getItem(getNotificationStorageKey(playerId));
    const parsed: unknown = value ? JSON.parse(value) : [];

    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string") : [];
  } catch {
    return [];
  }
}

export function writeDismissedNotifications(playerId: number, notificationIds: string[]) {
  localStorage.setItem(getNotificationStorageKey(playerId), JSON.stringify(notificationIds));
  window.dispatchEvent(new Event(NOTIFICATION_DISMISSALS_CHANGED));
}
