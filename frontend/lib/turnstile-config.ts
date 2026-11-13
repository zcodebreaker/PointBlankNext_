const TURNSTILE_TEST_SITE_KEY = "1x00000000000000000000AA";

export function getTurnstileSiteKey() {
  const configuredSiteKey = process.env.TURNSTILE_SITE_KEY?.trim();

  if (configuredSiteKey) return configuredSiteKey;
  return process.env.NODE_ENV === "development" ? TURNSTILE_TEST_SITE_KEY : "";
}
