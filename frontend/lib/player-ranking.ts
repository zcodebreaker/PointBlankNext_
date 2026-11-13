import "server-only";

import { cookies } from "next/headers";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

export type PlayerRankingEntry = {
  position: number;
  nickname: string;
  rank: number;
  experience: number;
  matches: number;
  wins: number;
  losses: number;
  kills: number;
  deaths: number;
  winRate: number;
};

export type PlayerRankingPage = {
  players: PlayerRankingEntry[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

/** Obtém uma página do ranking geral de jogadores sem reutilizar dados desatualizados. */
export async function getPlayerRanking(page: number, size = 20, keyword = ""): Promise<PlayerRankingPage> {
  try {
    const parameters = new URLSearchParams({ page: String(page), size: String(size) });
    if (keyword.trim()) parameters.set("keyword", keyword.trim());

    const response = await fetch(`${API_URL}/api/v1/rankings/players?${parameters}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    return response.json() as Promise<PlayerRankingPage>;
  } catch {
    return { players: [], page: 0, size, totalElements: 0, totalPages: 0 };
  }
}

/** Obtém a posição do jogador autenticado sem expor identificadores de outras contas. */
export async function getCurrentPlayerRanking(): Promise<PlayerRankingEntry | null> {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) return null;

  try {
    const response = await fetch(`${API_URL}/api/v1/rankings/players/me`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if ([401, 403, 404].includes(response.status)) return null;
    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    return response.json() as Promise<PlayerRankingEntry>;
  } catch {
    return null;
  }
}
