package com.pointblank.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

class AdminProfileServiceTests {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AdminProfileRepository profiles = mock(AdminProfileRepository.class);
    private final AdminProfileService service = new AdminProfileService(accounts, profiles);

    @BeforeEach
    void setup() {
        when(accounts.findByPlayerId(7L)).thenReturn(Optional.of(account(5, false)));
        when(profiles.find(7L)).thenReturn(AdminProfile.empty());
    }

    @Test
    void preservesOriginalBytesDimensionsAndRealFormat() throws Exception {
        for (String format : new String[]{"png", "jpeg"}) {
            byte[] original = image(format, 2048, 1024);
            service.update(7L, new UpdateAdminProfileRequest("  Administrador  ", false),
                    new MockMultipartFile("photo", "avatar.bin", "application/octet-stream", original));
            var captor = ArgumentCaptor.forClass(AdminProfileImage.class);
            verify(profiles).saveName(7L, "Administrador");
            verify(profiles).saveImage(eq(7L), captor.capture());
            assertThat(captor.getValue().data()).isEqualTo(original);
            assertThat(captor.getValue().width()).isEqualTo(2048);
            assertThat(captor.getValue().height()).isEqualTo(1024);
            assertThat(captor.getValue().contentType()).isEqualTo("image/" + format);
            assertThat(captor.getValue().version()).hasSize(64);
            clearInvocations(profiles);
        }
    }

    @Test
    void nicknameOnlyDoesNotReplacePhotoAndRemovalIsExplicit() {
        service.update(7L, new UpdateAdminProfileRequest("Meu apelido", false), null);
        verify(profiles, never()).saveImage(anyLong(), any());
        verify(profiles, never()).removeImage(anyLong());
        service.update(7L, new UpdateAdminProfileRequest("", true), null);
        verify(profiles).removeImage(7L);
        verify(profiles).saveName(7L, "");
    }

    @Test
    void rejectsRegularPlayersAndBannedAdministratorsBeforeAnyProfileAccess() {
        for (Account account : new Account[]{account(4, false), account(5, true)}) {
            when(accounts.findByPlayerId(7L)).thenReturn(Optional.of(account));
            assertForbidden(() -> service.find(7L));
            assertForbidden(() -> service.findImage(7L));
            assertForbidden(() -> service.update(7L, new UpdateAdminProfileRequest("nome", false), null));
        }
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsInvalidOversizedAndExcessiveDimensionImagesBeforeSaving() throws Exception {
        for (byte[] bytes : new byte[][]{new byte[0], "<svg onload='alert(1)'/>".getBytes(),
                new byte[10 * 1024 * 1024 + 1], image("png", 10001, 1)}) {
            assertThatThrownBy(() -> service.update(7L, new UpdateAdminProfileRequest("nome", false),
                    new MockMultipartFile("photo", "fake.png", "image/png", bytes)))
                    .isInstanceOfSatisfying(AdminAccountException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_REQUEST));
        }
        verifyNoInteractions(profiles);
    }

    @Test
    void rejectsInvalidNamesAndConflictingPhotoActions() {
        for (String name : new String[]{null, "n".repeat(33), "nome\nnovo"}) {
            assertThatThrownBy(() -> service.update(7L, new UpdateAdminProfileRequest(name, false), null))
                    .isInstanceOf(AdminAccountException.class);
        }
        assertThatThrownBy(() -> service.update(7L, new UpdateAdminProfileRequest("nome", true),
                new MockMultipartFile("photo", new byte[]{1}))).isInstanceOf(AdminAccountException.class);
        verifyNoInteractions(profiles);
    }

    @Test
    void missingPhotoReturnsNotFound() {
        when(profiles.findImage(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findImage(7L)).isInstanceOfSatisfying(AdminAccountException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(AdminAccountException.class,
                e -> assertThat(e.status()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    private static Account account(int level, boolean banned) {
        return new Account(7L, "admin", null, "GameNick", 40, 0, 100, 100, level, banned);
    }

    private static byte[] image(String format, int width, int height) throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), format, out);
        return out.toByteArray();
    }
}
