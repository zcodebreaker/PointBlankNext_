package com.pointblank.api.ranking;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClanRankingService {

    private final ClanRankingRepository clanRankingRepository;

    /**
     * Cria o serviço de ranking de clãs.
     *
     * @param clanRankingRepository repositório de clãs
     */
    public ClanRankingService(ClanRankingRepository clanRankingRepository) {
        this.clanRankingRepository = clanRankingRepository;
    }

    /**
     * Retorna uma página válida do ranking geral de clãs.
     *
     * @param requestedPage página solicitada, iniciada em zero
     * @param size quantidade de itens por página
     * @return página de ranking com totais
     */
    @Transactional(readOnly = true)
    public ClanRankingPage findGeneralRanking(int requestedPage, int size) {
        long totalElements = clanRankingRepository.countAll();
        int totalPages = totalElements == 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);
        int page = totalPages == 0 ? 0 : Math.min(requestedPage, totalPages - 1);
        List<ClanRankingEntry> clans = totalElements == 0
                ? List.of()
                : clanRankingRepository.findPage(page, size);

        return new ClanRankingPage(clans, page, size, totalElements, totalPages);
    }
}
