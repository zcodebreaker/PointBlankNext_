import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

import { AUTH_COOKIE } from "@/lib/auth-constants";

export function proxy(request: NextRequest) {
  if (!request.cookies.has(AUTH_COOKIE)) {
    return NextResponse.redirect(
      new URL("/login/form", process.env.POINTBLANK_SITE_URL ?? request.url),
    );
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/game/profile/:path*",
    "/mypage/info/:path*",
    "/mypage/notifications/:path*",
    "/ticket/:path*",
  ],
};
