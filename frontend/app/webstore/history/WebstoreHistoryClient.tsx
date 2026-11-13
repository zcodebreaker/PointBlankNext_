"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { logoutAccount } from "@/app/actions/auth";
import { getPlayerRank } from "@/lib/player-ranks";
import type { WebstoreDeliveryStatus, WebstoreHistoryItem } from "@/lib/webstore";
import styles from "../index/webstore.module.css";

type HistoryAccount = {
  username: string;
  rank: number;
  gold: number;
  cash: number;
};

type WebstoreHistoryClientProps = {
  account: HistoryAccount;
  months: 1 | 3 | 6;
  purchases: WebstoreHistoryItem[];
  startDate: string;
  endDate: string;
};

const numberFormatter = new Intl.NumberFormat("pt-BR");
const dateFormatter = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
  timeZone: "America/Sao_Paulo",
});

export default function WebstoreHistoryClient({
  account,
  months,
  purchases,
  startDate,
  endDate,
}: WebstoreHistoryClientProps) {
  const [isHeaderScrolled, setIsHeaderScrolled] = useState(false);
  const [history, setHistory] = useState(purchases);
  const [redeemablePurchases, setRedeemablePurchases] = useState<number[]>([]);
  const [pendingPurchaseId, setPendingPurchaseId] = useState<number | null>(null);
  const [deliveryMessage, setDeliveryMessage] = useState("");
  const [deliveryError, setDeliveryError] = useState("");
  const accountRank = getPlayerRank(account.rank);

  useEffect(() => {
    const updateHeader = () => setIsHeaderScrolled(window.scrollY > 12);
    updateHeader();
    window.addEventListener("scroll", updateHeader, { passive: true });
    return () => window.removeEventListener("scroll", updateHeader);
  }, []);

  const handleDelivery = async (purchaseId: number) => {
    const shouldRedeem = redeemablePurchases.includes(purchaseId);
    const action = shouldRedeem ? "redeem" : "verify";
    setPendingPurchaseId(purchaseId);
    setDeliveryMessage("");
    setDeliveryError("");

    try {
      const response = await fetch(`/api/webstore/purchases/${purchaseId}/${action}`, { method: "POST" });
      const result = (await response.json()) as WebstoreDeliveryStatus & { detail?: string; title?: string };
      if (!response.ok) throw new Error(result.detail ?? result.title ?? "Não foi possível verificar a entrega.");

      setHistory((current) => current.map((purchase) => purchase.purchaseId === purchaseId
        ? {
            ...purchase,
            delivered: result.delivered,
            canRedeem: result.canRedeem,
            recoveryCount: result.recoveryCount,
          }
        : purchase));
      setRedeemablePurchases((current) => result.canRedeem && !result.delivered
        ? [...new Set([...current, purchaseId])]
        : current.filter((id) => id !== purchaseId));
      setDeliveryMessage(result.message);
    } catch (error) {
      setDeliveryError(error instanceof Error ? error.message : "Não foi possível verificar a entrega.");
    } finally {
      setPendingPurchaseId(null);
    }
  };

  return (
    <div className={styles.store}>
      <header className={`${styles.header} ${isHeaderScrolled ? styles.headerScrolled : ""}`}>
        <div className={styles.headerTop}>
          <Link href="/" className={styles.gameLogo} aria-label="Voltar para o site Point Blank">
            <Image src="/images/common/site-main-logo-v3.png" alt="Point Blank" width={187} height={28} preload />
          </Link>

          <div className={styles.accountInfo}>
            <Link href="/game/profile" className={styles.accountUser} title={accountRank?.name}>
              {accountRank ? <Image src={accountRank.image} alt={accountRank.name} width={20} height={20} /> : null}
              <strong>{account.username}</strong>
            </Link>
            <div className={styles.cashBalance}>
              <Link href="/mypage/info"><strong>{numberFormatter.format(account.cash)}</strong></Link>
            </div>
            <div className={styles.goldBalance}>
              <Link href="/mypage/info"><strong>{numberFormatter.format(account.gold)}</strong></Link>
            </div>
          </div>
        </div>

        <div className={styles.utilityArea}>
          <div className={styles.utilityButtons}>
            <Link href="/webstore/index" className={styles.historyButton}>Loja</Link>
            <form action={logoutAccount}>
              <button type="submit" className={styles.loginButton}>Sair</button>
            </form>
          </div>
        </div>
      </header>

      <main className={`${styles.main} ${styles.historyMain}`}>
        <section className={styles.historyPanel} aria-labelledby="history-title">
          <div className={styles.historyHeading}>
            <p>PREMIUM SHOP</p>
            <h1 id="history-title">Histórico</h1>
            <span>Consulte as compras realizadas pela sua conta.</span>
          </div>

          <div className={styles.historyFilters}>
            <nav className={styles.periodTabs} aria-label="Período do histórico">
              {([1, 3, 6] as const).map((period) => (
                <Link
                  href={`/webstore/history?period=${period}`}
                  className={months === period ? styles.activePeriod : ""}
                  aria-current={months === period ? "page" : undefined}
                  key={period}
                >
                  {period} {period === 1 ? "Mês" : "Meses"}
                </Link>
              ))}
            </nav>
            <div className={styles.dateRange} aria-label={`Período de ${startDate} até ${endDate}`}>
              <time>{startDate}</time>
              <span aria-hidden="true" />
              <time>{endDate}</time>
            </div>
          </div>

          {deliveryMessage ? <div className={styles.success} role="status">{deliveryMessage}</div> : null}
          {deliveryError ? <div className={styles.error} role="alert">{deliveryError}</div> : null}

          {history.length > 0 ? (
            <ol className={styles.historyList}>
              {history.map((purchase) => {
                const isRedeemable = redeemablePurchases.includes(purchase.purchaseId);
                const isPending = pendingPurchaseId === purchase.purchaseId;
                return (
                <li className={styles.historyCard} key={purchase.purchaseId}>
                  <div className={styles.historyImage}>
                    {purchase.imagePath ? (
                      <Image src={purchase.imagePath} alt="" fill sizes="110px" />
                    ) : (
                      <span aria-hidden="true">PB</span>
                    )}
                  </div>
                  <div className={styles.historyProduct}>
                    <span>COMPRA #{purchase.purchaseId}</span>
                    <h2>{purchase.itemName}</h2>
                    <p>ID {purchase.itemId} · {purchase.countLabel}</p>
                  </div>
                  <div className={styles.historyDate}>
                    <span>Data</span>
                    <time dateTime={purchase.purchasedAt}>{dateFormatter.format(new Date(purchase.purchasedAt))}</time>
                  </div>
                  <div className={styles.historyValue}>
                    <span>Valor</span>
                    <strong>
                      <Image src="/images/webstore/coin_pb.png" alt="Cash" width={21} height={21} />
                      {numberFormatter.format(purchase.priceCash)}
                    </strong>
                    <small>Saldo: {numberFormatter.format(purchase.cashAfter)}</small>
                    <span className={`${styles.deliveryBadge} ${purchase.delivered ? styles.deliveredBadge : styles.missingBadge}`}>
                      {purchase.delivered ? "Entregue" : "Verificação necessária"}
                    </span>
                    <button
                      type="button"
                      className={`${styles.deliveryButton} ${purchase.delivered ? styles.verifiedDeliveryButton : ""}`}
                      disabled={isPending || purchase.delivered || purchase.recoveryCount >= 1}
                      onClick={() => handleDelivery(purchase.purchaseId)}
                    >
                      {isPending
                        ? "Verificando..."
                        : purchase.delivered
                          ? "Verificado"
                          : isRedeemable
                          ? "Resgatar novamente"
                          : purchase.recoveryCount >= 1
                            ? "Resgate já utilizado"
                            : "Verificar entrega"}
                    </button>
                  </div>
                </li>
                );
              })}
            </ol>
          ) : (
            <div className={styles.noHistory}>
              <strong>SEM DADOS.</strong>
              <span>Nenhuma compra foi encontrada no período selecionado.</span>
            </div>
          )}
        </section>
      </main>

      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <span className={styles.footerLogo}>
            <Image src="/images/common/logo_kryonix_ntw.png" alt="Kryonix Network" width={160} height={58} />
          </span>
          <div className={styles.copyright}>Copyright Kryonix Network. Todos direitos reservados</div>
          <div className={styles.ageRating}>
            <Image src="/images/webstore/age18.png" alt="Classificação indicativa: 18 anos" width={502} height={288} />
          </div>
        </div>
      </footer>
    </div>
  );
}
