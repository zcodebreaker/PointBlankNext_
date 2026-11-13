import { cookies } from "next/headers";
import { NextResponse } from "next/server";
import { API_URL } from "@/lib/auth-session";
import { AUTH_COOKIE } from "@/lib/auth-constants";

type DeliveryRouteContext = {
  params: Promise<{ purchaseId: string; action: string }>;
};

export async function POST(_request: Request, { params }: DeliveryRouteContext) {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) {
    return NextResponse.json(
      { title: "Login necessário", detail: "Entre na sua conta para verificar a entrega." },
      { status: 401 },
    );
  }

  const { purchaseId, action } = await params;
  const normalizedPurchaseId = Number(purchaseId);
  if (!Number.isSafeInteger(normalizedPurchaseId) || normalizedPurchaseId <= 0) {
    return NextResponse.json(
      { title: "Compra inválida", detail: "Não foi possível identificar esta compra." },
      { status: 400 },
    );
  }
  if (action !== "verify" && action !== "redeem") {
    return NextResponse.json(
      { title: "Ação inválida", detail: "A ação solicitada não está disponível." },
      { status: 404 },
    );
  }

  try {
    const response = await fetch(
      `${API_URL}/api/v1/webstore/purchases/${normalizedPurchaseId}/${action}`,
      {
        method: "POST",
        headers: {
          Accept: "application/json",
          Authorization: `Bearer ${token}`,
        },
        cache: "no-store",
      },
    );
    const responseBody = await response.text();
    return new Response(responseBody, {
      status: response.status,
      headers: { "Content-Type": response.headers.get("Content-Type") ?? "application/json" },
    });
  } catch {
    return NextResponse.json(
      { title: "Loja indisponível", detail: "Não foi possível verificar a entrega agora." },
      { status: 503 },
    );
  }
}
