"use client";

import type { MouseEvent } from "react";

export default function TopButton({ className }: { className: string }) {
  function scrollToTop(event: MouseEvent<HTMLAnchorElement>) {
    event.preventDefault();
    window.scrollTo({ top: 0, left: 0, behavior: "smooth" });
  }

  return (
    <a className={className} href="#" onClick={scrollToTop} aria-label="Voltar ao topo">
      TOP
    </a>
  );
}
