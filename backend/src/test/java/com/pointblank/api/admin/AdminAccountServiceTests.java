package com.pointblank.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

class AdminAccountServiceTests {

    private AccountRepository accountRepository;
    private AdminAccountDetailsRepository accountDetailsRepository;
    private AdminAccountService adminAccountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountDetailsRepository = mock(AdminAccountDetailsRepository.class);
        adminAccountService = new AdminAccountService(accountRepository, accountDetailsRepository);
    }

    @Test
    void findsAccountByPlayerId() {
        AdminAccountDetailsResponse details = accountDetails();
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountDetailsRepository.findByPlayerId(35L)).thenReturn(Optional.of(details));

        AdminAccountDetailsResponse response = adminAccountService.findOne(1L, "35");

        assertThat(response.username()).isEqualTo("player_1");
        assertThat(response.matches()).isEqualTo(42);
        verify(accountDetailsRepository).findByPlayerId(35L);
        verify(accountDetailsRepository, never()).findByUsername("35");
    }

    @Test
    void findsAccountByUsernameIgnoringSurroundingSpaces() {
        AdminAccountDetailsResponse details = accountDetails();
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountDetailsRepository.findByUsername("player_1")).thenReturn(Optional.of(details));

        AdminAccountDetailsResponse response = adminAccountService.findOne(1L, "  player_1  ");

        assertThat(response.playerId()).isEqualTo(35L);
        verify(accountDetailsRepository).findByUsername("player_1");
    }

    @Test
    void rejectsEmptyAccountLookup() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));

        assertThatThrownBy(() -> adminAccountService.findOne(1L, "   "))
                .isInstanceOfSatisfying(AdminAccountException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void listsAccountsForAdministrator() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.countAll("player")).thenReturn(1L);
        when(accountRepository.findAll("player", 0, 20)).thenReturn(List.of(player()));

        AdminAccountPageResponse response = adminAccountService.findAll(1L, "player", 0, 20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.accounts()).singleElement()
                .satisfies(account -> assertThat(account.username()).isEqualTo("player_1"));
    }

    @Test
    void rejectsAdministrativeOperationFromRegularPlayer() {
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player()));

        assertThatThrownBy(() -> adminAccountService.findAll(35L, "", 0, 20))
                .isInstanceOfSatisfying(AdminAccountException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void updatesGoldAndCash() {
        Account updated = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 250000, 80000, 0, false);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(35L))
                .thenReturn(Optional.of(player()))
                .thenReturn(Optional.of(updated));
        when(accountRepository.updateGold(35L, 250000)).thenReturn(true);
        when(accountRepository.updateCash(35L, 80000)).thenReturn(true);

        AdminAccountResponse response = adminAccountService.updateCurrencies(
                1L, 35L, new UpdateAccountCurrenciesRequest(250000, 80000));

        assertThat(response.gold()).isEqualTo(250000);
        assertThat(response.cash()).isEqualTo(80000);
        verify(accountRepository).updateGold(35L, 250000);
        verify(accountRepository).updateCash(35L, 80000);
    }

    @Test
    void bansRegularAccountUsingPbUserBlockState() {
        Account banned = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, true);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(35L))
                .thenReturn(Optional.of(player()))
                .thenReturn(Optional.of(banned));
        when(accountRepository.updateBanStatus(35L, true)).thenReturn(true);

        AdminAccountResponse response = adminAccountService.updateBanStatus(
                1L, 35L, new UpdateAccountBanRequest(true));

        assertThat(response.banned()).isTrue();
        verify(accountRepository).updateBanStatus(35L, true);
    }

    @Test
    void updatesCashWithoutBillingProvider() {
        Account updated = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 250000, 90000, 0, false);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(35L))
                .thenReturn(Optional.of(player()))
                .thenReturn(Optional.of(updated));
        when(accountRepository.updateGold(35L, 250000)).thenReturn(true);
        when(accountRepository.updateCash(35L, 90000)).thenReturn(true);

        AdminAccountResponse response = adminAccountService.updateCurrencies(
                1L, 35L, new UpdateAccountCurrenciesRequest(250000, 90000));

        assertThat(response.cash()).isEqualTo(90000);
        verify(accountRepository).updateGold(35L, 250000);
        verify(accountRepository).updateCash(35L, 90000);
    }

    @Test
    void addsGoldAndCashToCurrentBalances() {
        Account updated = new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 125000, 85000, 0, false);
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(35L))
                .thenReturn(Optional.of(player()))
                .thenReturn(Optional.of(updated));
        when(accountRepository.addCurrencies(35L, 25000, 5000)).thenReturn(true);

        AdminAccountResponse response = adminAccountService.addCurrencies(
                1L, 35L, new AddAccountCurrenciesRequest(25000, 5000));

        assertThat(response.gold()).isEqualTo(125000);
        assertThat(response.cash()).isEqualTo(85000);
        verify(accountRepository).addCurrencies(35L, 25000, 5000);
    }

    @Test
    void rejectsEmptyCurrencyGrant() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(35L)).thenReturn(Optional.of(player()));

        assertThatThrownBy(() -> adminAccountService.addCurrencies(
                1L, 35L, new AddAccountCurrenciesRequest(0, 0)))
                .isInstanceOfSatisfying(AdminAccountException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(accountRepository, never()).addCurrencies(35L, 0, 0);
    }

    @Test
    void protectsAdministrativeAccountsFromBan() {
        when(accountRepository.findByPlayerId(1L)).thenReturn(Optional.of(administrator()));
        when(accountRepository.findByPlayerId(2L)).thenReturn(Optional.of(
                new Account(2L, "admin_2", "admin2@example.com", "Admin2", 0, 0, 0, 0, 5, false)));

        assertThatThrownBy(() -> adminAccountService.updateBanStatus(
                1L, 2L, new UpdateAccountBanRequest(true)))
                .isInstanceOfSatisfying(AdminAccountException.class, exception ->
                        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT));
    }

    private Account administrator() {
        return new Account(1L, "admin", "admin@example.com", "Admin", 0, 0, 0, 0, 5, false);
    }

    private Account player() {
        return new Account(
                35L, "player_1", "player@example.com", "Soldado1",
                1, 1200, 100000, 80000, 0, false);
    }

    private AdminAccountDetailsResponse accountDetails() {
        return new AdminAccountDetailsResponse(
                35L,
                "player_1",
                "player@example.com",
                "Soldado1",
                1,
                1200,
                100000,
                80000,
                0,
                false,
                false,
                2610152038L,
                42,
                25,
                15,
                2,
                310,
                190,
                80,
                120);
    }
}
