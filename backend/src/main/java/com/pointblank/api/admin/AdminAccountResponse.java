package com.pointblank.api.admin;

import com.pointblank.api.account.Account;

public record AdminAccountResponse(
        long playerId,
        String username,
        String email,
        String nickname,
        int rank,
        long experience,
        int gold,
        int cash,
        int accessLevel,
        boolean banned) {

    /**
     * Converte uma conta para o formato exibido no painel administrativo.
     *
     * @param account conta de origem
     * @return representação administrativa da conta
     */
    public static AdminAccountResponse from(Account account) {
        return new AdminAccountResponse(
                account.playerId(),
                account.username(),
                account.email(),
                account.nickname(),
                account.rank(),
                account.experience(),
                account.gold(),
                account.cash(),
                account.accessLevel(),
                account.banned());
    }
}
