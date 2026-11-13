import "server-only";

import { API_URL } from "@/lib/auth-session";

export type ClanRankingEntry = {
  position: number;
  clanId: number;
  name: string;
  leader: string;
  memberCount: number;
  rank: number;
  experience: number;
  logo: number;
  wins: number;
  losses: number;
  winRate: number;
};

export type ClanRankingPage = {
  clans: ClanRankingEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export async function getClanRanking(page: number, size = 20): Promise<ClanRankingPage> {
  try {
    const response = await fetch(`${API_URL}/api/v1/rankings/clans?page=${page}&size=${size}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    return response.json() as Promise<ClanRankingPage>;
  } catch {
    return { clans: [], page: 0, size, totalElements: 0, totalPages: 0 };
  }
}
