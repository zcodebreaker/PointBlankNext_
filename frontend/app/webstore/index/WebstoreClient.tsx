"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { logoutAccount } from "@/app/actions/auth";
import { getPlayerRank } from "@/lib/player-ranks";
import type { WebstoreItem, WebstorePurchase } from "@/lib/webstore";
import styles from "./webstore.module.css";

type StoreAccount = {
  username: string;
  rank: number;
  gold: number;
  cash: number;
};

type WebstoreClientProps = {
  items: WebstoreItem[];
  account: StoreAccount | null;
  purchasedItemIds: number[];
};

type ProblemDetail = {
  title?: string;
  detail?: string;
};

const numberFormatter = new Intl.NumberFormat("pt-BR");
const categories = [
  { value: "ALL", label: "Todos" },
  { value: "WEAPON", label: "Armas" },
  { value: "CHARACTER", label: "Personagens" },
  { value: "ITEM", label: "Itens" },
] as const;

export default function WebstoreClient({
  items,
  account,
  purchasedItemIds: initialPurchasedItemIds,
}: WebstoreClientProps) {
  const [category, setCategory] = useState<(typeof categories)[number]["value"]>("ALL");
  const [search, setSearch] = useState("");
  const [selectedItem, setSelectedItem] = useState<WebstoreItem | null>(null);
  const [purchaseRequestId, setPurchaseRequestId] = useState("");
  const [cash, setCash] = useState(account?.cash ?? 0);
  const [buying, setBuying] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [duplicateMessage, setDuplicateMessage] = useState("");
  const [purchasedItemIds, setPurchasedItemIds] = useState(initialPurchasedItemIds);
  const [isHeaderScrolled, setIsHeaderScrolled] = useState(false);
  const accountRank = account ? getPlayerRank(account.rank) : null;
  const purchasedItems = useMemo(() => new Set(purchasedItemIds), [purchasedItemIds]);

  useEffect(() => {
    const updateHeader = () => setIsHeaderScrolled(window.scrollY > 12);

    updateHeader();
    window.addEventListener("scroll", updateHeader, { passive: true });
    return () => window.removeEventListener("scroll", updateHeader);
  }, []);

  const filteredItems = useMemo(() => {
    const term = search.trim().toLocaleLowerCase("pt-BR");
    return items.filter((item) => {
      const matchesCategory = category === "ALL" || item.category === category;
      const matchesSearch = !term || item.name.toLocaleLowerCase("pt-BR").includes(term);
      return matchesCategory && matchesSearch;
    });
  }, [category, items, search]);

  const openPurchase = (item: WebstoreItem) => {
    setMessage("");
    setError("");
    setDuplicateMessage("");
    setPurchaseRequestId(crypto.randomUUID());
    setSelectedItem(item);
  };

  const closePurchase = () => {
    if (!buying) {
      setSelectedItem(null);
      setPurchaseRequestId("");
    }
  };

  const purchase = async () => {
    if (!selectedItem || !purchaseRequestId) return;
    if (!account) {
      window.location.href = "/login/form";
      return;
    }

    setBuying(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch("/api/webstore/purchases", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          goodId: selectedItem.goodId,
          requestId: purchaseRequestId,
        }),
      });

      if (!response.ok) {
        const problem = (await response.json()) as ProblemDetail;
        const detail = problem.detail ?? problem.title ?? "Não foi possível concluir a compra.";
        if (response.status === 409 && detail.toLocaleLowerCase("pt-BR").includes("já foi adquirido")) {
          setPurchasedItemIds((current) => current.includes(selectedItem.itemId)
            ? current
            : [...current, selectedItem.itemId]);
          setSelectedItem(null);
          setPurchaseRequestId("");
          setDuplicateMessage(detail);
          return;
        }
        throw new Error(detail);
      }

      const result = (await response.json()) as WebstorePurchase;
      setCash(result.cashBalance);
      setPurchasedItemIds((current) => current.includes(result.itemId) ? current : [...current, result.itemId]);
      setSelectedItem(null);
      setPurchaseRequestId("");
      setMessage(`${result.itemName} (${result.countLabel}) foi entregue ao seu inventário.`);
    } catch (purchaseError) {
      setError(purchaseError instanceof Error ? purchaseError.message : "Não foi possível concluir a compra.");
    } finally {
      setBuying(false);
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
            {account ? (
              <>
                <Link href="/game/profile" className={styles.accountUser} title={accountRank?.name}>
                  {accountRank ? <Image src={accountRank.image} alt={accountRank.name} width={20} height={20} /> : null}
                  <strong>{account.username}</strong>
                </Link>
                <div className={styles.cashBalance}>
                  <Link href="/mypage/info">
                    <strong>{numberFormatter.format(cash)}</strong>
                  </Link>
                </div>
                <div className={styles.goldBalance}>
                  <Link href="/mypage/info"><strong>{numberFormatter.format(account.gold)}</strong></Link>
                </div>
              </>
            ) : (
              <>
                <div className={styles.cashBalance}><span className={styles.emptyBalance}>-</span></div>
                <div className={styles.goldBalance}><span className={styles.emptyBalance}>-</span></div>
              </>
            )}
          </div>
        </div>

        <div className={styles.utilityArea}>
          <div className={styles.utilityButtons}>
            <Link href="/webstore/history" className={styles.historyButton}>Histórico</Link>
            {account ? (
              <form action={logoutAccount}>
                <button type="submit" className={styles.loginButton}>Sair</button>
              </form>
            ) : (
              <Link href="/login/form" className={styles.loginButton}>Login</Link>
            )}
          </div>
        </div>
      </header>

      <main className={styles.main}>
        <section className={styles.hero}>
          <p>POINT BLANK</p>
          <h1>PREMIUM SHOP</h1>
          <span>Equipe sua conta e receba os itens diretamente no inventário.</span>
        </section>

        <section className={styles.catalog} aria-labelledby="catalog-title">
          <div className={styles.catalogHeading}>
            <div>
              <p>CATÁLOGO</p>
              <h2 id="catalog-title">Itens Premium</h2>
            </div>
            <label className={styles.search}>
              <span className={styles.srOnly}>Procurar produto</span>
              <input
                type="search"
                value={search}
                maxLength={50}
                placeholder="Procurar..."
                onChange={(event) => setSearch(event.target.value)}
              />
            </label>
          </div>

          <div className={styles.filters} aria-label="Filtrar por categoria">
            {categories.map((entry) => (
              <button
                type="button"
                className={category === entry.value ? styles.activeFilter : ""}
                aria-pressed={category === entry.value}
                onClick={() => setCategory(entry.value)}
                key={entry.value}
              >
                {entry.label}
              </button>
            ))}
          </div>

          {message ? <div className={styles.success} role="status">{message}</div> : null}
          {error && !selectedItem ? <div className={styles.error} role="alert">{error}</div> : null}

          {filteredItems.length > 0 ? (
            <ul className={styles.productGrid}>
              {filteredItems.map((item) => {
                const isPurchased = Boolean(account && purchasedItems.has(item.itemId));
                return (
                  <li className={`${styles.productCard} ${isPurchased ? styles.purchasedProduct : ""}`} key={item.goodId}>
                    <div className={styles.productImage}>
                      <Image src={item.imagePath} alt={item.name} fill sizes="(max-width: 680px) 90vw, (max-width: 1100px) 45vw, 300px" />
                      {item.tagLabel ? <span className={styles.tag}>{item.tagLabel}</span> : null}
                    </div>
                    <div className={styles.productBody}>
                      <span className={styles.category}>{item.categoryLabel}</span>
                      <h3>{item.name}</h3>
                      <div className={styles.productMeta}>
                        <span>{item.countLabel}</span>
                        <strong className={styles.price}>
                          <Image src="/images/webstore/coin_pb.png" alt="Cash" width={24} height={24} />
                          {numberFormatter.format(item.priceCash)}
                        </strong>
                      </div>
                      <button type="button" disabled={isPurchased} onClick={() => openPurchase(item)}>
                        {isPurchased ? "Adquirido" : account ? "Comprar agora" : "Entrar para comprar"}
                      </button>
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : (
            <div className={styles.empty}>Nenhum produto encontrado neste filtro.</div>
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

      {selectedItem ? (
        <div className={styles.modalBackdrop} role="presentation" onMouseDown={closePurchase}>
          <section
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="purchase-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button type="button" className={styles.modalClose} aria-label="Fechar" disabled={buying} onClick={closePurchase}>×</button>
            <div className={styles.modalImage}>
              <Image src={selectedItem.imagePath} alt={selectedItem.name} fill sizes="280px" />
            </div>
            <div className={styles.modalContent}>
              <span className={styles.category}>{selectedItem.categoryLabel}</span>
              <h2 id="purchase-title">{selectedItem.name}</h2>
              <p>Você receberá <strong>{selectedItem.countLabel}</strong> deste item no inventário.</p>
              <div className={styles.purchaseSummary}>
                <span>Valor da compra</span>
                <strong><Image src="/images/webstore/coin_pb.png" alt="Cash" width={24} height={24} />{numberFormatter.format(selectedItem.priceCash)}</strong>
              </div>
              {account ? (
                <div className={styles.purchaseSummary}>
                  <span>Saldo após a compra</span>
                  <strong>{numberFormatter.format(Math.max(0, cash - selectedItem.priceCash))} Cash</strong>
                </div>
              ) : null}
              {error ? <div className={styles.error} role="alert">{error}</div> : null}
              <div className={styles.modalActions}>
                <button type="button" className={styles.cancelButton} disabled={buying} onClick={closePurchase}>Cancelar</button>
                <button type="button" disabled={buying || Boolean(account && cash < selectedItem.priceCash)} onClick={purchase}>
                  {buying ? "Processando..." : account && cash < selectedItem.priceCash ? "Cash insuficiente" : "Confirmar compra"}
                </button>
              </div>
            </div>
          </section>
        </div>
      ) : null}

      {duplicateMessage ? (
        <div className={styles.modalBackdrop} role="presentation" onMouseDown={() => setDuplicateMessage("")}>
          <section
            className={styles.noticeModal}
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="duplicate-purchase-title"
            aria-describedby="duplicate-purchase-message"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <span className={styles.noticeIcon} aria-hidden="true">!</span>
            <p>PREMIUM SHOP</p>
            <h2 id="duplicate-purchase-title">Item já adquirido</h2>
            <span id="duplicate-purchase-message">{duplicateMessage}. Cada item pode ser comprado apenas uma vez por conta.</span>
            <button type="button" autoFocus onClick={() => setDuplicateMessage("")}>Entendi</button>
          </section>
        </div>
      ) : null}
    </div>
  );
}
