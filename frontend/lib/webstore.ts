import "server-only";

import { cookies } from "next/headers";
import { API_URL } from "@/lib/auth-session";
import { AUTH_COOKIE } from "@/lib/auth-constants";

export type WebstoreItem = {
  goodId: number;
  itemId: number;
  name: string;
  priceCash: number;
  count: number;
  countLabel: string;
  category: "WEAPON" | "CHARACTER" | "ITEM";
  categoryLabel: string;
  tag: number;
  tagLabel: string;
  imagePath: string;
};

export type WebstorePurchase = {
  purchaseId: number;
  goodId: number;
  itemId: number;
  itemName: string;
  priceCash: number;
  itemCount: number;
  countLabel: string;
  cashBalance: number;
  purchasedAt: string;
  repeated: boolean;
};

export type WebstoreHistoryItem = {
  purchaseId: number;
  goodId: number;
  itemId: number;
  itemName: string;
  priceCash: number;
  itemCount: number;
  countLabel: string;
  cashAfter: number;
  purchasedAt: string;
  imagePath: string | null;
  delivered: boolean;
  canRedeem: boolean;
  recoveryCount: number;
};

export type WebstoreDeliveryStatus = {
  purchaseId: number;
  delivered: boolean;
  canRedeem: boolean;
  recoveryCount: number;
  message: string;
};

export async function getWebstoreItems(): Promise<WebstoreItem[]> {
  try {
    const response = await fetch(`${API_URL}/api/v1/webstore/items`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });

    if (!response.ok) {
      throw new Error(`Não foi possível carregar a loja (HTTP ${response.status}).`);
    }

    return response.json() as Promise<WebstoreItem[]>;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Não foi possível carregar")) throw error;
    throw new Error("A API da Premium Shop está indisponível.", { cause: error });
  }
}

export async function getWebstoreHistory(months: 1 | 3 | 6): Promise<WebstoreHistoryItem[]> {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) return [];

  try {
    const response = await fetch(`${API_URL}/api/v1/webstore/purchases?months=${months}`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if (!response.ok) {
      throw new Error(`Não foi possível carregar o histórico (HTTP ${response.status}).`);
    }

    return response.json() as Promise<WebstoreHistoryItem[]>;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Não foi possível carregar")) throw error;
    throw new Error("A API da Premium Shop está indisponível.", { cause: error });
  }
}

export async function getPurchasedWebstoreItemIds(): Promise<number[]> {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) return [];

  try {
    const response = await fetch(`${API_URL}/api/v1/webstore/purchases/items`, {
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      cache: "no-store",
    });

    if (response.status === 401) return [];
    if (!response.ok) {
      throw new Error(`Não foi possível carregar os itens adquiridos (HTTP ${response.status}).`);
    }

    return response.json() as Promise<number[]>;
  } catch (error) {
    if (error instanceof Error && error.message.startsWith("Não foi possível carregar")) throw error;
    throw new Error("A API da Premium Shop está indisponível.", { cause: error });
  }
}
