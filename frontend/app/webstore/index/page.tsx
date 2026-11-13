import type { Metadata } from "next";
import { getCurrentAccount } from "@/lib/auth-session";
import { getPurchasedWebstoreItemIds, getWebstoreItems } from "@/lib/webstore";
import WebstoreClient from "./WebstoreClient";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Compre itens para a sua conta Point Blank.",
};

export default async function WebstorePage() {
  const [items, account, purchasedItemIds] = await Promise.all([
    getWebstoreItems(),
    getCurrentAccount(),
    getPurchasedWebstoreItemIds(),
  ]);

  return (
    <WebstoreClient
      items={items}
      purchasedItemIds={purchasedItemIds}
      account={account ? {
        username: account.username,
        rank: account.rank,
        gold: account.gold,
        cash: account.cash,
      } : null}
    />
  );
}
