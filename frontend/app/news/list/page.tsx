import type { Metadata } from "next";
import Link from "next/link";
import CookieConsent from "@/components/CookieConsent";
import SiteHeader from "@/components/ConnectedSiteHeader";
import NewsCard from "@/components/NewsCard";
import QuickMenu from "@/components/QuickMenu";
import SiteFooter from "@/components/SiteFooter";
import TopButton from "@/components/TopButton";
import { getHeaderSession } from "@/lib/auth-session";
import { getPublishedNews, type NewsCategory } from "@/lib/news";

export const metadata: Metadata = {
  title: "POINT BLANK",
  description: "Notícias, eventos, promoções e atualizações do POINT BLANK.",
};

const NEWS_LIST_PATH = "/news/list";

const tabs: Array<{ label: string; value: "" | NewsCategory }> = [
  { label: "Todos", value: "" },
  { label: "Promoção", value: "PROMO" },
  { label: "Evento", value: "EVENT" },
  { label: "Patch", value: "PATCH" },
  { label: "E-Sports", value: "ESPORTS" },
  { label: "Informações", value: "INFO" },
];

type NewsPageProps = {
  searchParams: Promise<{ categoria?: string; limite?: string }>;
};

export default async function NewsPage({ searchParams }: NewsPageProps) {
  const [session, storedNews, parameters] = await Promise.all([
    getHeaderSession(),
    getPublishedNews(),
    searchParams,
  ]);
  const requestedCategory = parameters.categoria?.toUpperCase() ?? "";
  const category = tabs.some((tab) => tab.value === requestedCategory) ? requestedCategory as "" | NewsCategory : "";
  const requestedLimit = Number(parameters.limite ?? 8);
  const limit = Number.isFinite(requestedLimit) ? Math.min(100, Math.max(8, requestedLimit)) : 8;
  const featured = storedNews.slice(0, 3);
  const filtered = category ? storedNews.filter((article) => article.category === category) : storedNews;
  const visible = filtered.slice(0, limit);

  return (
    <div id="all_wrap">
      <SiteHeader username={session?.username} accessLevel={session?.accessLevel} />
      <QuickMenu />
      <div className="pb_content">
        <div className="pb_sub">
          <div className="pb_news">
            <div className="pb_top">
              <div className="typing_wrap">
                <div className="typing-txt">NOTÍCIAS</div>
                <div className="typing">NOTÍCIAS</div>
              </div>
            </div>
            <div className="pb_sub_con">
              <div className="nw_wrap">
                <div className="nw_list">
                  <div className="inner">
                    <ul className="list featured-news-list">{featured.map((article) => <NewsCard key={article.newsId} article={article} />)}</ul>
                  </div>
                </div>
                <div className="nw_all">
                  <div className="inner">
                    <div className="nw_depth2 nsb_depth2">
                      <span className="nw_tab">{tabs.find((tab) => tab.value === category)?.label ?? "Todos"}</span>
                      <ul>
                        {tabs.map((tab) => (
                          <li className={tab.value === category ? "on" : undefined} key={tab.label}>
                            <Link href={tab.value ? `${NEWS_LIST_PATH}?categoria=${tab.value}` : NEWS_LIST_PATH}>{tab.label}</Link>
                          </li>
                        ))}
                      </ul>
                    </div>
                    <ul className="list devList">{visible.map((article) => <NewsCard key={article.newsId} article={article} />)}</ul>
                    <Link
                      className="btn_read"
                      href={visible.length < filtered.length
                        ? `${NEWS_LIST_PATH}?${category ? `categoria=${category}&` : ""}limite=${limit + 8}`
                        : category ? `${NEWS_LIST_PATH}?categoria=${category}` : NEWS_LIST_PATH}
                    >
                      VER DETALHES
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <CookieConsent />
      <SiteFooter />
      <TopButton className="go_top" />
    </div>
  );
}
