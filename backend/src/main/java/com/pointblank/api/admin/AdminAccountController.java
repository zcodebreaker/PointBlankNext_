package com.pointblank.api.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    /**
     * Cria o controlador administrativo de contas.
     *
     * @param adminAccountService serviço de gerenciamento de contas
     */
    public AdminAccountController(AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    /**
     * Lista as contas disponíveis no painel administrativo.
     *
     * @param jwt token do administrador autenticado
     * @param search filtro textual opcional
     * @param page índice da página, iniciado em zero
     * @param size quantidade máxima de itens
     * @return página de contas
     */
    @GetMapping
    public AdminAccountPageResponse findAll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminAccountService.findAll(playerId(jwt), search, page, size);
    }

    /**
     * Busca uma única conta pelo Player ID ou pelo nome de usuário exato.
     *
     * @param jwt token do administrador autenticado
     * @param query Player ID ou nome de usuário
     * @return detalhes completos e seguros da conta
     */
    @GetMapping("/lookup")
    public AdminAccountDetailsResponse findOne(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String query) {
        return adminAccountService.findOne(playerId(jwt), query);
    }

    /**
     * Atualiza os saldos de Gold e Cash de uma conta.
     *
     * @param jwt token do administrador autenticado
     * @param playerId identificador da conta alvo
     * @param request novos saldos
     * @return conta atualizada
     */
    @PatchMapping("/{playerId}/currencies")
    public AdminAccountResponse updateCurrencies(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long playerId,
            @Valid @RequestBody UpdateAccountCurrenciesRequest request) {
        return adminAccountService.updateCurrencies(playerId(jwt), playerId, request);
    }

    /**
     * Adiciona Gold e Cash aos saldos atuais de uma conta.
     *
     * @param jwt token do administrador autenticado
     * @param playerId identificador da conta alvo
     * @param request valores que serão adicionados
     * @return conta atualizada
     */
    @PatchMapping("/{playerId}/currency-grants")
    public AdminAccountResponse addCurrencies(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long playerId,
            @Valid @RequestBody AddAccountCurrenciesRequest request) {
        return adminAccountService.addCurrencies(playerId(jwt), playerId, request);
    }

    /**
     * Altera o estado de banimento de uma conta.
     *
     * @param jwt token do administrador autenticado
     * @param playerId identificador da conta alvo
     * @param request estado de banimento desejado
     * @return conta atualizada
     */
    @PatchMapping("/{playerId}/ban")
    public AdminAccountResponse updateBanStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @Positive long playerId,
            @Valid @RequestBody UpdateAccountBanRequest request) {
        return adminAccountService.updateBanStatus(playerId(jwt), playerId, request);
    }

    /**
     * Extrai o identificador do administrador do subject do JWT.
     *
     * @param jwt token da requisição autenticada
     * @return identificador do administrador
     */
    private long playerId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new AdminAccountException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }
}
