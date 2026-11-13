import { permanentRedirect } from "next/navigation";

export default async function ArticlePage({ params }: { params: Promise<{ newsId: string }> }) {
  const { newsId } = await params;
  permanentRedirect(`/noticias/view?idx=${encodeURIComponent(newsId)}&page=1`);
}
