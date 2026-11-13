import "server-only";

import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { AUTH_ACCESS_LEVEL_COOKIE, AUTH_COOKIE, AUTH_USERNAME_COOKIE } from "@/lib/auth-constants";

export const API_URL = process.env.POINTBLANK_API_URL ?? "http://localhost:8080";

export async function hasAuthSession() {
  try {
    return (await getCurrentAccount()) !== null;
  } catch {
    return false;
  }
}

export type AccountProfile = {
  playerId: number;
  username: string;
  email: string;
  rank: number;
  experience: number;
  gold: number;
  cash: number;
  accessLevel: number;
};

export type HeaderSession = {
  username: string;
  accessLevel: number | null;
};

export const getCurrentAccount = cache(async (): Promise<AccountProfile | null> => {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) return null;

  try {
    const response = await fetch(`${API_URL}/api/v1/accounts/me`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if ([401, 403, 404].includes(response.status)) return null;
    if (!response.ok) throw new Error(`Não foi possível carregar a conta (HTTP ${response.status}).`);

    return response.json() as Promise<AccountProfile>;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Não foi possível carregar")) throw error;
    throw new Error("A API do Point Blank está indisponível.", { cause: error });
  }
});

export async function requireCurrentAccount() {
  const account = await getCurrentAccount();

  if (!account) redirect("/login/form");

  return account;
}

export const getHeaderSession = cache(async (): Promise<HeaderSession | null> => {
  const cookieStore = await cookies();
  const token = cookieStore.get(AUTH_COOKIE)?.value;

  if (!token) return null;

  const storedUsername = cookieStore.get(AUTH_USERNAME_COOKIE)?.value.trim();
  const accessLevelValue = cookieStore.get(AUTH_ACCESS_LEVEL_COOKIE)?.value;
  const storedAccessLevel = accessLevelValue === undefined ? null : Number(accessLevelValue);

  try {
    const account = await getCurrentAccount();
    if (!account) return null;

    const username = account.username?.trim();

    if (!username) return null;

    return {
      username,
      accessLevel: Number.isFinite(account.accessLevel) ? account.accessLevel : null,
    };
  } catch {
    return storedUsername
      ? { username: storedUsername, accessLevel: Number.isFinite(storedAccessLevel) ? storedAccessLevel : null }
      : null;
  }
});

export const getAuthUsername = cache(async (): Promise<string | null> => {
  return (await getHeaderSession())?.username ?? null;
});
