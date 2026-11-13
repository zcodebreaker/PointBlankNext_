package com.pointblank.api.account;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pointblank.api.auth.BannedAccountException;

/** Expõe o perfil de jogo somente para a própria conta autenticada. */
@RestController
@RequestMapping("/api/v1/accounts/me/game-profile")
public class GameProfileController {

    private final AccountRepository accountRepository;
    private final GameProfileRepository gameProfileRepository;

    public GameProfileController(
            AccountRepository accountRepository,
            GameProfileRepository gameProfileRepository) {
        this.accountRepository = accountRepository;
        this.gameProfileRepository = gameProfileRepository;
    }

    @GetMapping
    public GameProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        long playerId = playerId(jwt);
        Account account = accountRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conta não encontrada"));

        if (account.banned()) throw new BannedAccountException();

        return gameProfileRepository.findByPlayerId(playerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil de jogo não encontrado"));
    }

    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido", exception);
        }
    }
}
