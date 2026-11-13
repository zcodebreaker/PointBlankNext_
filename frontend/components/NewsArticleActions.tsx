"use client";

import Link from "next/link";
import { useState, type MouseEvent } from "react";

export default function NewsArticleActions() {
  const [copied, setCopied] = useState(false);

  function shareOnFacebook(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    const target = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(window.location.href)}`;
    window.open(target, "pointblank-facebook-share", "popup,width=600,height=500,scrollbars=yes");
  }

  async function copyLink(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    } catch {
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNode(document.body);
      selection?.removeAllRanges();
      selection?.addRange(range);
      document.execCommand("copy");
      selection?.removeAllRanges();
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1800);
    }
  }

  return (
    <div className="nw_v_btns">
      <a href="https://www.facebook.com/sharer/sharer.php" className="btn_line_b btn_share" onClick={shareOnFacebook}>
        <i />COMPARTILHAR NO
      </a>
      <a href="#" className="btn_line_b btn_link" onClick={copyLink} aria-live="polite">
        <i />{copied ? "LINK COPIADO" : "COPIAR LINK"}
      </a>
      <Link href="/news/list" className="btn_line_r">Lista</Link>
    </div>
  );
}
