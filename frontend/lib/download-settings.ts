import "server-only";

import { API_URL } from "@/lib/auth-session";

export type DownloadSettings = {
  fileUrl: string;
  clientVersion: string;
  mirrorTorrentUrl: string;
  mirrorGoogleDriveUrl: string;
  mirrorOneDriveUrl: string;
  patchPrimaryUrl: string;
  patchSecondaryUrl: string;
  fileSizeBytes: number;
  fileUpdatedAt: string;
  metadataCheckedAt: string;
};

const fallbackSettings: DownloadSettings = {
  fileUrl: "https://download-br.zepetto.com/pointblank/ZepettoDownloader_v1.3.exe",
  clientVersion: "0.2.2",
  mirrorTorrentUrl: "https://download-br.zepetto.com/pointblank/PointBlank_SetupFull_20260209.torrent",
  mirrorGoogleDriveUrl: "https://drive.google.com/file/d/1cx2YhZuzUH74RSNJL9WZ6eYmVGmAuJA_/view?usp=sharing",
  mirrorOneDriveUrl: "https://1drv.ms/u/c/9cf48022fcd0db06/IQCpI3CHfrbSRbUfwpefRfhbAYt6GFc5SV6o20p5wH8d8d8?e=5zFhZR",
  patchPrimaryUrl: "https://download-br.zepetto.com/pointblank/Patch_PointBlank_Ver261.exe",
  patchSecondaryUrl: "https://download-br.zepetto.com/pointblank/Patch_PointBlank_Ver260.exe",
  fileSizeBytes: 16_192_026_706,
  fileUpdatedAt: "2026-03-25T00:00:00Z",
  metadataCheckedAt: "2026-03-25T00:00:00Z",
};

export async function getDownloadSettings(): Promise<DownloadSettings> {
  try {
    const response = await fetch(`${API_URL}/api/v1/downloads/client`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    if (!response.ok) return fallbackSettings;
    return response.json() as Promise<DownloadSettings>;
  } catch {
    return fallbackSettings;
  }
}

export function formatDownloadSize(bytes: number) {
  const units = ["B", "KB", "MB", "GB", "TB"];
  let value = Math.max(0, bytes);
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${new Intl.NumberFormat("en-US", { maximumFractionDigits: 2 }).format(value)} ${units[unitIndex]}`;
}

export function formatDownloadDate(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toISOString().slice(0, 10);
}
