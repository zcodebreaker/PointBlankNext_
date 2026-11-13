"use client";

import { useEffect, useState } from "react";

type CookiePreferences = {
  essential: boolean;
  analytics: boolean;
  functional: boolean;
};

type StoredCookieConsent = CookiePreferences & {
  version: number;
};

const CONSENT_COOKIE_NAME = "pb_cookie_consent";
const CONSENT_COOKIE_MAX_AGE = 60 * 60 * 24 * 365;
const CONSENT_VERSION = 1;
const defaultPreferences: CookiePreferences = {
  essential: true,
  analytics: false,
  functional: false,
};

const cookieTypes = [
  { id: "essential", title: "Cookies Essenciais", text: "São utilizados para reconhecer você ao acessar e proteger sua conta. Eles são indispensáveis para o funcionamento do nosso site e, portanto, não podem ser desativados.", required: true },
  { id: "analytics", title: "Cookies analíticos", text: "Eles nos ajudam a entender como você navega pelo site e permitem otimizar sua experiência.", required: false },
  { id: "functional", title: "Cookies funcionais", text: "Proporcionam funcionalidades avançadas e permitem a integração com serviços de terceiros.", required: false },
] as const satisfies ReadonlyArray<{ id: keyof CookiePreferences; title: string; text: string; required: boolean }>;

function readStoredConsent(): CookiePreferences | null {
  const storedCookie = document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith(`${CONSENT_COOKIE_NAME}=`));

  if (!storedCookie) {
    return null;
  }

  try {
    const stored = JSON.parse(decodeURIComponent(storedCookie.slice(CONSENT_COOKIE_NAME.length + 1))) as Partial<StoredCookieConsent>;

    if (stored.version !== CONSENT_VERSION) {
      return null;
    }

    return {
      essential: true,
      analytics: stored.analytics === true,
      functional: stored.functional === true,
    };
  } catch {
    return null;
  }
}

function storeConsent(preferences: CookiePreferences) {
  const value = encodeURIComponent(JSON.stringify({ version: CONSENT_VERSION, ...preferences }));
  const secure = window.location.protocol === "https:" ? "; Secure" : "";
  document.cookie = `${CONSENT_COOKIE_NAME}=${value}; Max-Age=${CONSENT_COOKIE_MAX_AGE}; Path=/; SameSite=Lax${secure}`;
}

export default function CookieConsent() {
  const [visible, setVisible] = useState(false);
  const [configOpen, setConfigOpen] = useState(false);
  const [preferences, setPreferences] = useState<CookiePreferences>(defaultPreferences);

  useEffect(() => {
    const stored = readStoredConsent();

    if (stored) {
      setPreferences(stored);
      return;
    }

    setVisible(true);
  }, []);

  const saveConsent = (nextPreferences: CookiePreferences) => {
    setPreferences(nextPreferences);
    storeConsent(nextPreferences);
    setConfigOpen(false);
    setVisible(false);
  };

  const acceptAll = () => {
    saveConsent({ essential: true, analytics: true, functional: true });
  };

  return (
    <>
      {visible && (
        <div className="cookie" role="region" aria-label="Preferências de cookies">
          <button type="button" className="close_ck" aria-label="Fechar aviso de cookies" onClick={() => saveConsent(preferences)}>Fechar</button>
          <div className="inner">
            <div className="txt">Nossos sites utilizam cookies para melhorar a sua segurança e experiência. Saiba mais na nossa <a href="#rodape">Política de Privacidade.</a></div>
            <div className="btns">
              <button type="button" className="btn_accept" onClick={acceptAll}>Aceitar Cookies</button>
              <button type="button" className="btn_config" onClick={() => setConfigOpen(true)}>Configurar Cookies</button>
            </div>
          </div>
        </div>
      )}

      {configOpen && (
        <>
          <button type="button" className="dimmed cookie-dimmed" aria-label="Fechar configurações" onClick={() => setConfigOpen(false)} />
          <div className="pop-up pop-cookie cookie-modal" role="dialog" aria-modal="true" aria-labelledby="cookie-title">
            <div className="pop-layout">
              <div className="wrap">
                <h2 id="cookie-title">Configurar Cookies <button type="button" className="pop-close icon-button" aria-label="Fechar" onClick={() => setConfigOpen(false)}>Fechar</button></h2>
                <div className="info">Utilizamos cookies para entender como você usa o nosso site.<br />Com essas informações, conseguimos melhorar o funcionamento do site e realizar análises.</div>
                <div className="pop-con">
                  <div className="pop-in">
                    <div className="cookie_list">
                      <ul>
                        {cookieTypes.map((cookie) => (
                          <li key={cookie.id}>
                            <div className="desc"><div className="tit">{cookie.title}</div><div className="txt">{cookie.text}</div></div>
                            <div className="controls">
                              <input
                                id={`cookie-${cookie.id}`}
                                type="checkbox"
                                checked={preferences[cookie.id]}
                                disabled={cookie.required}
                                onChange={(event) => setPreferences((current) => ({ ...current, [cookie.id]: event.target.checked }))}
                              />
                              <label htmlFor={`cookie-${cookie.id}`}><span>on</span></label>
                            </div>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
                <div className="cookie-save-row"><button type="button" className="btn_save" onClick={() => saveConsent(preferences)}>Salvar configurações</button></div>
              </div>
            </div>
          </div>
        </>
      )}
    </>
  );
}
