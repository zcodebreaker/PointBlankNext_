import type { Metadata } from "next";
import { requireCurrentAccount } from "@/lib/auth-session";
import { getWebstoreHistory } from "@/lib/webstore";
import WebstoreHistoryClient from "./WebstoreHistoryClient";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Histórico de compras realizadas na Premium Shop do Point Blank.",
};

type HistoryPageProps = {
  searchParams: Promise<{ period?: string | string[] }>;
};

export default async function WebstoreHistoryPage({ searchParams }: HistoryPageProps) {
  const account = await requireCurrentAccount();
  const rawPeriod = (await searchParams).period;
  const periodValue = Array.isArray(rawPeriod) ? rawPeriod[0] : rawPeriod;
  const months = periodValue === "3" || periodValue === "6" ? Number(periodValue) as 3 | 6 : 1;
  const purchases = await getWebstoreHistory(months);
  const endDate = new Date();
  const startDate = new Date(endDate);
  startDate.setMonth(startDate.getMonth() - months);

  return (
    <WebstoreHistoryClient
      account={{
        username: account.username,
        rank: account.rank,
        gold: account.gold,
        cash: account.cash,
      }}
      months={months}
      purchases={purchases}
      startDate={formatDate(startDate)}
      endDate={formatDate(endDate)}
    />
  );
}

function formatDate(value: Date) {
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    timeZone: "America/Sao_Paulo",
  }).format(value);
}
