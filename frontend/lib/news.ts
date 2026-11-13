import "server-only";

import { API_URL } from "@/lib/auth-session";

export type NewsCategory = "PROMO" | "EVENT" | "PATCH" | "ESPORTS" | "INFO";

export type NewsArticle = {
  newsId: number;
  authorId: number;
  authorUsername: string;
  category: NewsCategory;
  title: string;
  summary: string;
  content: string;
  coverUrl: string;
  carouselUrl: string;
  bannerUrl: string;
  published: boolean;
  publishedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export async function getPublishedNews(): Promise<NewsArticle[]> {
  try {
    const response = await fetch(`${API_URL}/api/v1/news?limit=100`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    if (!response.ok) return [];
    return response.json() as Promise<NewsArticle[]>;
  } catch {
    return [];
  }
}

export async function getPublishedArticle(newsId: number): Promise<NewsArticle | null> {
  try {
    const response = await fetch(`${API_URL}/api/v1/news/${newsId}`, {
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    if (!response.ok) return null;
    return response.json() as Promise<NewsArticle>;
  } catch {
    return null;
  }
}
