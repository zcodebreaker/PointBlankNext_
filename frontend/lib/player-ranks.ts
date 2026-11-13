export type PlayerRank = {
  name: string;
  image: string;
};

const rankNames = [
  "Novato", "Recruta", "Soldado", "Cabo", "Sargento",
  "Terceiro Sargento - Grau 1", "Terceiro Sargento - Grau 2", "Terceiro Sargento - Grau 3",
  "Segundo Sargento - Grau 1", "Segundo Sargento - Grau 2", "Segundo Sargento - Grau 3", "Segundo Sargento - Grau 4",
  "Primeiro Sargento - Grau 1", "Primeiro Sargento - Grau 2", "Primeiro Sargento - Grau 3", "Primeiro Sargento - Grau 4", "Primeiro Sargento - Grau 5",
  "Segundo Tenente - Grau 1", "Segundo Tenente - Grau 2", "Segundo Tenente - Grau 3", "Segundo Tenente - Grau 4",
  "Primeiro Tenente - Grau 1", "Primeiro Tenente - Grau 2", "Primeiro Tenente - Grau 3", "Primeiro Tenente - Grau 4", "Primeiro Tenente - Grau 5",
  "Capitão - Grau 1", "Capitão - Grau 2", "Capitão - Grau 3", "Capitão - Grau 4", "Capitão - Grau 5",
  "Major - Grau 1", "Major - Grau 2", "Major - Grau 3", "Major - Grau 4", "Major - Grau 5",
  "Tenente-Coronel - Grau 1", "Tenente-Coronel - Grau 2", "Tenente-Coronel - Grau 3", "Tenente-Coronel - Grau 4", "Tenente-Coronel - Grau 5",
  "Coronel - Grau 1", "Coronel - Grau 2", "Coronel - Grau 3", "Coronel - Grau 4", "Coronel - Grau 5",
  "General de Brigada", "General de Divisão", "General de Exército", "Marechal", "Herói de Guerra", "Lendário",
] as const;

/** Traduz o índice de patente armazenado pelo servidor para o nome e a insígnia pública. */
export function getPlayerRank(rank: number, iconSet: "official" | "profile" = "official"): PlayerRank {
  const index = Math.max(0, Math.min(rankNames.length - 1, Math.trunc(rank) || 0));
  const imageName = index < 10 ? `rank_0${index}.png` : `rank_${index}.png`;
  const imageFolder = iconSet === "profile" ? "rank" : "official";

  return {
    name: rankNames[index],
    image: `/images/common/icon_ranking/${imageFolder}/${imageName}`,
  };
}
