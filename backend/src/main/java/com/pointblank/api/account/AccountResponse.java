package com.pointblank.api.account;

public record AccountResponse
(
    long playerId,
    String username,
    String email,
    int rank,
    long experience,
    int gold,
    int cash,
    int accessLevel
    ){

    /**
     * Converte a entidade de conta no formato público retornado pela API.
     *
     * @param account conta de origem
     * @return resposta sem os dados sensíveis da conta
     */
    public static AccountResponse from(Account account)
    {
        return new AccountResponse
        (
           account.playerId(),
           account.username(),
           account.email(),
           account.rank(),
           account.experience(),
           account.gold(),
           account.cash(),
           account.accessLevel()
        );
    }
}
