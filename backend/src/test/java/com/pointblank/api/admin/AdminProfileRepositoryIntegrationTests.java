package com.pointblank.api.admin;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.pointblank.api.account.AccountRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@EnabledIfEnvironmentVariable(named = "RUN_DB_TESTS", matches = "true")
class AdminProfileRepositoryIntegrationTests {
    @Autowired private AdminProfileRepository profiles;
    @Autowired private AccountRepository accounts;

    @Test
    void storesOriginalBytesPreservesPhotoOnRenameAndIsolatesAccounts() {
        String name = "prof_" + UUID.randomUUID().toString().substring(0, 8);
        var account = accounts.create(name, "test-only-password", name + "@example.invalid");
        long id = account.playerId();
        assertThat(profiles.find(id)).isEqualTo(AdminProfile.empty());
        byte[] original = new byte[]{0, 1, 2, (byte) 255, 4, 5};
        profiles.saveName(id, "Apelido de teste");
        profiles.saveImage(id, new AdminProfileImage(original, "image/png", 2048, 1024, "a".repeat(64)));
        profiles.saveName(id, "Outro apelido");
        assertThat(profiles.find(id)).isEqualTo(new AdminProfile("Outro apelido", "a".repeat(64), 2048, 1024, 6L));
        assertThat(profiles.findImage(id).orElseThrow().data()).isEqualTo(original);
        assertThat(profiles.find(Long.MAX_VALUE)).isEqualTo(AdminProfile.empty());
        assertThat(profiles.findImage(Long.MAX_VALUE)).isEmpty();
        assertThat(accounts.findByPlayerId(id).orElseThrow().nickname()).isEqualTo(account.nickname());
        profiles.removeImage(id);
        assertThat(profiles.findImage(id)).isEmpty();
        assertThat(profiles.find(id)).isEqualTo(new AdminProfile("Outro apelido", null, null, null, null));
    }
}
