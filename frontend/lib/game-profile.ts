import "server-only";

import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

export type GameProfilePlayer = {
  playerId: number;
  nickname: string;
  rank: number;
  online: boolean;
};

export type GameProfile = {
  playerId: number;
  username: string;
  accessLevel: number;
  nickname: string;
  rank: number;
  experience: number;
  matches: number;
  wins: number;
  losses: number;
  draws: number;
  kills: number;
  assists: number;
  deaths: number;
  headshots: number;
  totalPlaytime: number;
  matchPlaytime: number;
  clanName: string;
  weapons: {
    assaultKills: number;
    assaultDeaths: number;
    smgKills: number;
    smgDeaths: number;
    sniperKills: number;
    sniperDeaths: number;
    mgKills: number;
    mgDeaths: number;
    shotgunKills: number;
    shotgunDeaths: number;
  };
  friends: GameProfilePlayer[];
  clanMembers: GameProfilePlayer[];
};

export const requireGameProfile = cache(async (): Promise<GameProfile> => {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) redirect("/login/form");

  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/v1/accounts/me/game-profile`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });
  } catch (error) {
    throw new Error("A API do Point Blank está indisponível.", { cause: error });
  }

  if ([401, 403, 404].includes(response.status)) redirect("/login/form");
  if (!response.ok) throw new Error(`Não foi possível carregar o perfil de jogo (HTTP ${response.status}).`);

  return response.json() as Promise<GameProfile>;
});
