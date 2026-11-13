"use server";

import { cookies } from "next/headers";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

type ApiProblem = {
  detail?: string;
  errors?: Record<string, string>;
};

export type PasswordActionResult =
  | { success: true; message: string }
  | { success: false; message: string; errors?: Record<string, string> };

export async function changePassword(formData: FormData): Promise<PasswordActionResult> {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) {
    return { success: false, message: "Sua sessão expirou. Entre novamente para alterar a senha." };
  }

  let response: Response;

  try {
    response = await fetch(`${API_URL}/api/v1/accounts/me/password`, {
      method: "PATCH",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        currentPassword: String(formData.get("currentPassword") ?? ""),
        newPassword: String(formData.get("newPassword") ?? ""),
        newPasswordConfirmation: String(formData.get("newPasswordConfirmation") ?? ""),
      }),
      cache: "no-store",
    });
  } catch {
    return { success: false, message: "A API está indisponível. Confirme se o Spring Boot está em execução." };
  }

  if (response.status === 401 || response.status === 403) {
    return { success: false, message: "Sua sessão expirou. Entre novamente para alterar a senha." };
  }

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ApiProblem;
    return {
      success: false,
      message: problem.detail ?? "Não foi possível alterar a senha.",
      errors: problem.errors,
    };
  }

  return { success: true, message: "Senha alterada com sucesso." };
}
