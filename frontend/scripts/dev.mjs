import { spawn } from "node:child_process";
import { existsSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const frontendDirectory = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const environment = {
  ...process.env,
  TURNSTILE_SITE_KEY: process.env.TURNSTILE_SITE_KEY ?? "1x00000000000000000000AA",
};

const services = [
  { name: "SITE", directory: frontendDirectory, args: ["dev"] },
];

const children = [];
let stopping = false;

function stop(exitCode) {
  if (stopping) return;
  stopping = true;

  for (const child of children) {
    if (child.pid && !child.killed) child.kill();
  }

  process.exitCode = exitCode;
}

for (const service of services) {
  const nextCli = resolve(service.directory, "node_modules", "next", "dist", "bin", "next");

  if (!existsSync(nextCli)) {
    console.error(`[${service.name}] Dependências ausentes em ${service.directory}. Execute npm install nessa pasta.`);
    stop(1);
    break;
  }

  const child = spawn(process.execPath, [nextCli, ...service.args], {
    cwd: service.directory,
    env: environment,
    stdio: "inherit",
  });

  children.push(child);

  child.on("error", (error) => {
    console.error(`[${service.name}] Não foi possível iniciar:`, error.message);
    stop(1);
  });

  child.on("exit", (code, signal) => {
    if (stopping) return;

    const reason = signal ? `sinal ${signal}` : `código ${code ?? 1}`;
    console.error(`[${service.name}] Serviço encerrado (${reason}).`);
    stop(code ?? 1);
  });
}

process.once("SIGINT", () => stop(0));
process.once("SIGTERM", () => stop(0));
