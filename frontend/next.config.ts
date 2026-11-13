import type { NextConfig } from "next";

const apiUrl = (process.env.POINTBLANK_API_URL ?? "http://localhost:8080").replace(/\/+$/, "");

const nextConfig: NextConfig = {
  reactStrictMode: true,
  turbopack: {
    root: process.cwd(),
  },
  async rewrites() {
    return [
      {
        source: "/api/v1/news/:path*",
        destination: `${apiUrl}/api/v1/news/:path*`,
      },
      {
        source: "/api/v1/webstore/:path*",
        destination: `${apiUrl}/api/v1/webstore/:path*`,
      },
    ];
  },
};

export default nextConfig;
