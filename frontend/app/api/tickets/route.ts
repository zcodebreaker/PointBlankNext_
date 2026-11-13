import { cookies } from "next/headers";
import { AUTH_COOKIE } from "@/lib/auth-constants";
import { API_URL } from "@/lib/auth-session";

export async function POST(request: Request) {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;

  if (!token) {
    return Response.json({ detail: "Sua sessão expirou. Entre novamente." }, { status: 401 });
  }

  try {
    const formData = await request.formData();
    const response = await fetch(`${API_URL}/api/v1/tickets`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: formData,
      cache: "no-store",
    });
    const body = await response.arrayBuffer();

    return new Response(body, {
      status: response.status,
      headers: {
        "Content-Type": response.headers.get("content-type") ?? "application/json; charset=utf-8",
      },
    });
  } catch {
    return Response.json(
      { detail: "A API está indisponível. Confirme se o Spring Boot está em execução." },
      { status: 503 },
    );
  }
}

