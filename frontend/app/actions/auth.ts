"use server";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import {
  AUTH_ACCESS_LEVEL_COOKIE,
  AUTH_COOKIE,
  AUTH_USERNAME_COOKIE,
} from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

type ApiProblem = {
  detail?: string;
  errors?: Record<string, string>;
};

type ApiAuthResponse = {
  accessToken: string;
  expiresIn: number;
  account: {
    username: string;
    accessLevel: number;
  };
};

type ApiRegistrationResponse = {
  message: string;
};

type AvailabilityApiResponse = {
  available: boolean;
};

export type AuthActionResult =
  | { success: true; message: string }
  | { success: false; message: string; errors?: Record<string, string> };

export type AvailabilityResult =
  | { success: true; available: boolean }
  | { success: false; message: string };

async function checkAvailability(path: string, parameter: string, candidate: string): Promise<AvailabilityResult> {
  try {
    const response = await fetch(
      `${API_URL}/api/v1/auth/${path}?${parameter}=${encodeURIComponent(candidate)}`,
      { cache: "no-store" },
    );

    if (!response.ok) {
      return { success: false, message: "Não foi possível verificar agora." };
    }

    const result = (await response.json()) as AvailabilityApiResponse;
    return { success: true, available: result.available };
  } catch {
    return { success: false, message: "A API está indisponível no momento." };
  }
}

export async function checkUsernameAvailability(username: string): Promise<AvailabilityResult> {
  const candidate = username.trim();
  if (!/^[A-Za-z0-9_]{4,16}$/.test(candidate)) {
    return { success: false, message: "Use de 4 a 16 caracteres com letras, números ou _." };
  }
  return checkAvailability("username-availability", "username", candidate);
}

export async function checkEmailAvailability(email: string): Promise<AvailabilityResult> {
  const candidate = email.trim().toLowerCase();
  if (candidate.length > 255 || !/^[^\s@]+@[^\s@]+$/.test(candidate)) {
    return { success: false, message: "Informe um e-mail válido." };
  }
  return checkAvailability("email-availability", "email", candidate);
}

async function authenticate(
  payload: Record<string, string | boolean>,
): Promise<AuthActionResult> {
  let response: Response;

  try {
    response = await fetch(`${API_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
  } catch {
    return {
      success: false,
      message: "A API está indisponível. Confirme se o Spring Boot está em execução.",
    };
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem;
    return {
      success: false,
      message: problem.detail ?? "Não foi possível concluir a solicitação.",
      errors: problem.errors,
    };
  }

  const auth = (await response.json()) as ApiAuthResponse;
  const cookieStore = await cookies();
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "lax",
    path: "/",
    maxAge: auth.expiresIn,
    priority: "high",
  } as const;
  cookieStore.set(AUTH_COOKIE, auth.accessToken, cookieOptions);
  cookieStore.set(AUTH_USERNAME_COOKIE, auth.account.username, cookieOptions);
  cookieStore.set(AUTH_ACCESS_LEVEL_COOKIE, String(auth.account.accessLevel), cookieOptions);
  return {
    success: true,
    message: `Acesso autorizado. Bem-vindo, ${auth.account.username}!`,
  };
}

export async function registerAccount(formData: FormData): Promise<AuthActionResult> {
  const payload = {
    username: String(formData.get("username") ?? ""),
    email: String(formData.get("email") ?? ""),
    password: String(formData.get("password") ?? ""),
    passwordConfirmation: String(formData.get("passwordConfirmation") ?? ""),
    turnstileToken: String(formData.get("turnstileToken") ?? ""),
    acceptedTerms: formData.get("terms") === "on",
  };

  let response: Response;
  try {
    response = await fetch(`${API_URL}/api/v1/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
    });
  } catch {
    return {
      success: false,
      message: "A API está indisponível. Confirme se o Spring Boot está em execução.",
    };
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem;
    return {
      success: false,
      message: problem.detail ?? "Não foi possível criar a conta.",
      errors: problem.errors,
    };
  }

  const registration = (await response.json()) as ApiRegistrationResponse;
  return { success: true, message: registration.message };
}

export async function loginAccount(formData: FormData): Promise<AuthActionResult> {
  const remember = formData.get("remember") === "on";
  return authenticate({
    username: String(formData.get("username") ?? ""),
    password: String(formData.get("password") ?? ""),
    turnstileToken: String(formData.get("turnstileToken") ?? ""),
    remember,
  });
}

export async function logoutAccount(): Promise<never> {
  const cookieStore = await cookies();
  cookieStore.delete(AUTH_COOKIE);
  cookieStore.delete(AUTH_USERNAME_COOKIE);
  cookieStore.delete(AUTH_ACCESS_LEVEL_COOKIE);
  redirect("/logout");
}
