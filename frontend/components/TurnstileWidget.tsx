"use client";

import Script from "next/script";
import { useCallback, useEffect, useRef, useState } from "react";

type TurnstileWidgetId = string;

type TurnstileRenderOptions = {
  sitekey: string;
  action: "login" | "register";
  theme: "dark";
  language: "pt-BR";
  size: "flexible";
  callback: (token: string) => void;
  "error-callback": () => void;
  "expired-callback": () => void;
};

type TurnstileApi = {
  render: (container: HTMLElement, options: TurnstileRenderOptions) => TurnstileWidgetId;
  reset: (widgetId: TurnstileWidgetId) => void;
  remove: (widgetId: TurnstileWidgetId) => void;
};

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

type TurnstileWidgetProps = {
  siteKey: string;
  action: "login" | "register";
  resetSignal: number;
  onTokenChange: (token: string) => void;
};

export default function TurnstileWidget({
  siteKey,
  action,
  resetSignal,
  onTokenChange,
}: TurnstileWidgetProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const widgetIdRef = useRef<TurnstileWidgetId | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);

  const renderWidget = useCallback(() => {
    if (!siteKey || !containerRef.current || !window.turnstile || widgetIdRef.current) return;

    widgetIdRef.current = window.turnstile.render(containerRef.current, {
      sitekey: siteKey,
      action,
      theme: "dark",
      language: "pt-BR",
      size: "flexible",
      callback: onTokenChange,
      "error-callback": () => {
        onTokenChange("");
        setLoadFailed(true);
      },
      "expired-callback": () => onTokenChange(""),
    });
  }, [action, onTokenChange, siteKey]);

  useEffect(() => {
    renderWidget();
  }, [renderWidget]);

  useEffect(() => {
    if (resetSignal === 0 || !widgetIdRef.current || !window.turnstile) return;

    window.turnstile.reset(widgetIdRef.current);
    onTokenChange("");
    setLoadFailed(false);
  }, [onTokenChange, resetSignal]);

  useEffect(() => () => {
    if (widgetIdRef.current && window.turnstile) {
      window.turnstile.remove(widgetIdRef.current);
      widgetIdRef.current = null;
    }
  }, []);

  if (!siteKey) {
    return <p className="turnstile-error" role="alert">A verificação de segurança não está configurada.</p>;
  }

  return (
    <div className="turnstile-widget">
      <Script
        id="cloudflare-turnstile"
        src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"
        strategy="afterInteractive"
        onReady={renderWidget}
        onError={() => setLoadFailed(true)}
      />
      <div ref={containerRef} className="turnstile-container" aria-label="Verificação de segurança Cloudflare Turnstile" />
      {loadFailed ? <p className="turnstile-error" role="alert">Não foi possível carregar a verificação. Tente novamente.</p> : null}
    </div>
  );
}
