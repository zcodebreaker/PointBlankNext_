package com.pointblank.api.ranking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClanRankingServiceTests {

    private ClanRankingRepository clanRankingRepository;
    private ClanRankingService clanRankingService;

    /** Prepara um serviço isolado antes de cada teste. */
    @BeforeEach
    void setUp() {
        clanRankingRepository = mock(ClanRankingRepository.class);
        clanRankingService = new ClanRankingService(clanRankingRepository);
    }

    /** Confirma a paginação e o retorno das posições do ranking. */
    @Test
    void listsGeneralClanRanking() {
        ClanRankingEntry clan = clan(21);
        when(clanRankingRepository.countAll()).thenReturn(21L);
        when(clanRankingRepository.findPage(1, 20)).thenReturn(List.of(clan));

        ClanRankingPage response = clanRankingService.findGeneralRanking(1, 20);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.clans()).containsExactly(clan);
    }

    /** Limita uma página acima do total à última página existente. */
    @Test
    void clampsRequestedPageToLastAvailablePage() {
        when(clanRankingRepository.countAll()).thenReturn(21L);
        when(clanRankingRepository.findPage(1, 20)).thenReturn(List.of(clan(21)));

        ClanRankingPage response = clanRankingService.findGeneralRanking(99, 20);

        assertThat(response.page()).isEqualTo(1);
        verify(clanRankingRepository).findPage(1, 20);
    }

    /** Evita consultar itens quando ainda não existem clãs. */
    @Test
    void returnsEmptyPageWithoutQueryingItems() {
        when(clanRankingRepository.countAll()).thenReturn(0L);

        ClanRankingPage response = clanRankingService.findGeneralRanking(0, 20);

        assertThat(response.clans()).isEmpty();
        assertThat(response.totalPages()).isZero();
    }

    /** Cria uma entrada de ranking reutilizada pelos testes. */
    private ClanRankingEntry clan(int position) {
        return new ClanRankingEntry(
                position, 15, "THUNDER", "Player2", 1, 3,
                520000L, 336464393L, 95, 55, 63);
    }
}
