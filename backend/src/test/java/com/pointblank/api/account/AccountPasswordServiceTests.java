package com.pointblank.api.account;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pointblank.api.auth.BannedAccountException;

class AccountPasswordServiceTests {

    private AccountRepository accountRepository;
    private AccountPasswordService accountPasswordService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountPasswordService = new AccountPasswordService(accountRepository);
    }

    @Test
    void changesPasswordAfterCheckingCurrentPassword() {
        when(accountRepository.passwordMatches(35L, "senha-atual")).thenReturn(true);
        when(accountRepository.updatePassword(35L, "nova-senha")).thenReturn(true);

        accountPasswordService.changePassword(
                35L,
                new ChangePasswordRequest("senha-atual", "nova-senha", "nova-senha"));

        verify(accountRepository).updatePassword(35L, "nova-senha");
    }

    @Test
    void rejectsIncorrectCurrentPassword() {
        when(accountRepository.passwordMatches(35L, "senha-errada")).thenReturn(false);

        assertThatThrownBy(() -> accountPasswordService.changePassword(
                35L,
                new ChangePasswordRequest("senha-errada", "nova-senha", "nova-senha")))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("A senha atual está incorreta");

        verify(accountRepository, never()).updatePassword(35L, "nova-senha");
    }

    @Test
    void rejectsDifferentNewPasswordConfirmation() {
        assertThatThrownBy(() -> accountPasswordService.changePassword(
                35L,
                new ChangePasswordRequest("senha-atual", "nova-senha", "outra-senha")))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("As novas senhas informadas não são iguais");

        verify(accountRepository, never()).passwordMatches(35L, "senha-atual");
    }

    @Test
    void rejectsReusingCurrentPassword() {
        when(accountRepository.passwordMatches(35L, "senha-atual")).thenReturn(true);

        assertThatThrownBy(() -> accountPasswordService.changePassword(
                35L,
                new ChangePasswordRequest("senha-atual", "senha-atual", "senha-atual")))
                .isInstanceOf(PasswordChangeException.class)
                .hasMessage("A nova senha deve ser diferente da senha atual");

        verify(accountRepository, never()).updatePassword(35L, "senha-atual");
    }

    @Test
    void rejectsPasswordChangeForBannedAccount() {
        when(accountRepository.isBanned(35L)).thenReturn(true);

        assertThatThrownBy(() -> accountPasswordService.changePassword(
                35L,
                new ChangePasswordRequest("senha-atual", "nova-senha", "nova-senha")))
                .isInstanceOf(BannedAccountException.class)
                .hasMessage("Esta conta está banida");

        verify(accountRepository, never()).passwordMatches(35L, "senha-atual");
    }
}
