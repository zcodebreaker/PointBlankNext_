// Valores de "onNextLevel" do RankClanTemplate.json usado pelo servidor.
// A soma acumulada define o XP mínimo de cada patente, do nível 0 ao 49.
const CLAN_RANK_XP_REQUIREMENTS = [
  24_000,
  40_000,
  48_000,
  56_000,
  72_000,
  80_000,
  120_000,
  160_000,
  120_000,
  200_000,
  240_000,
  280_000,
  320_000,
  360_000,
  360_000,
  400_000,
  480_000,
  560_000,
  640_000,
  720_000,
  560_000,
  640_000,
  720_000,
  800_000,
  880_000,
  960_000,
  1_870_000,
  1_120_000,
  1_200_000,
  1_280_000,
  1_600_000,
  1_620_000,
  1_440_000,
  2_560_000,
  2_880_000,
  3_200_000,
  3_520_000,
  3_840_000,
  4_160_000,
  4_480_000,
  4_800_000,
  5_120_000,
  5_440_000,
  5_760_000,
  6_080_000,
  6_800_000,
  7_200_000,
  7_600_000,
  9_200_000,
] as const;

const CLAN_RANK_XP_THRESHOLDS = CLAN_RANK_XP_REQUIREMENTS.reduce<number[]>(
  (thresholds, requiredExperience) => [
    ...thresholds,
    thresholds[thresholds.length - 1] + requiredExperience,
  ],
  [0],
);

export type ClanRank = {
  level: number;
  image: string;
  minimumExperience: number;
  nextLevelExperience: number | null;
};

export function getClanRank(experience: number): ClanRank {
  const normalizedExperience = Number.isFinite(experience)
    ? Math.max(0, Math.trunc(experience))
    : 0;
  let level = 0;

  for (let candidate = 1; candidate < CLAN_RANK_XP_THRESHOLDS.length; candidate += 1) {
    if (normalizedExperience < CLAN_RANK_XP_THRESHOLDS[candidate]) {
      break;
    }

    level = candidate;
  }

  return {
    level,
    image: `/images/common/icon_ranking/clan/clan_${level}.png`,
    minimumExperience: CLAN_RANK_XP_THRESHOLDS[level],
    nextLevelExperience: CLAN_RANK_XP_THRESHOLDS[level + 1] ?? null,
  };
}
