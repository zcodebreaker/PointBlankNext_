package com.pointblank.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LegacyPasswordHasherTests {

    @Test
    void createsTheSameUtf8HmacMd5UsedByPointBlankServer() {
        LegacyPasswordHasher hasher = new LegacyPasswordHasher();

        assertThat(hasher.hash("abc"))
                .isEqualTo("9a4fccd06f8c188f540391fa0416c651");
    }
}
