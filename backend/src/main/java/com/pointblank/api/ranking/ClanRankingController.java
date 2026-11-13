package com.pointblank.api.ranking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/rankings/clans")
public class ClanRankingController {

    private final ClanRankingService clanRankingService;

    /**
     * Cria o controlador público do ranking de clãs.
     *
     * @param clanRankingService serviço de ranking
     */
    public ClanRankingController(ClanRankingService clanRankingService) {
        this.clanRankingService = clanRankingService;
    }

    /**
     * Lista o ranking geral de clãs com paginação.
     *
     * @param page índice da página, iniciado em zero
     * @param size quantidade de clãs por página
     * @return página do ranking geral
     */
    @GetMapping
    public ClanRankingPage findGeneralRanking(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return clanRankingService.findGeneralRanking(page, size);
    }
}
