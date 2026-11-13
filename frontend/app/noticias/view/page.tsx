import type { Metadata } from "next";
import { notFound } from "next/navigation";
import NewsArticleView from "@/app/noticias/_components/NewsArticleView";

export const metadata: Metadata = { title: "POINT BLANK" };

type NewsViewPageProps = {
  searchParams: Promise<{
    idx?: string | string[];
    page?: string | string[];
  }>;
};

function firstValue(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

export default async function NewsViewPage({ searchParams }: NewsViewPageProps) {
  const parameters = await searchParams;
  const newsId = Number(firstValue(parameters.idx));
  const requestedPage = Number(firstValue(parameters.page) ?? "1");
  if (!Number.isSafeInteger(newsId) || newsId === 0) notFound();
  const page = Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1;

  return <NewsArticleView newsId={newsId} page={page} />;
}
