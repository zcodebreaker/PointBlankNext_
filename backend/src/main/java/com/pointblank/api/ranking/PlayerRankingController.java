package com.pointblank.api.ranking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Expõe o ranking público de jogadores para o site.
 */
@Validated
@RestController
@RequestMapping("/api/v1/rankings/players")
public class PlayerRankingController {

    private final PlayerRankingService playerRankingService;

    /**
     * Cria o controlador público do ranking de jogadores.
     *
     * @param playerRankingService serviço de ranking
     */
    public PlayerRankingController(PlayerRankingService playerRankingService) {
        this.playerRankingService = playerRankingService;
    }

    /**
     * Lista o ranking geral de jogadores com paginação.
     *
     * @param page índice da página, iniciado em zero
     * @param size quantidade de jogadores por página
     * @param keyword apelido opcional a pesquisar
     * @return página do ranking geral
     */
    @GetMapping
    public PlayerRankingPage findGeneralRanking(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "") @Size(max = 32) String keyword) {
        return playerRankingService.findGeneralRanking(page, size, keyword);
    }

    /**
     * Retorna a posição real do jogador autenticado, quando ele é elegível ao
     * ranking público.
     *
     * @param jwt token JWT validado pelo Spring Security
     * @return entrada pessoal no ranking
     */
    @GetMapping("/me")
    public PlayerRankingEntry findCurrentPlayerRanking(@AuthenticationPrincipal Jwt jwt) {
        return playerRankingService.findCurrentPlayerRanking(playerId(jwt))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Jogador não participa do ranking"));
    }

    /**
     * Obtém o identificador numérico do jogador a partir do subject do token.
     *
     * @param jwt token JWT autenticado
     * @return identificador do jogador
     */
    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido", exception);
        }
    }
}
