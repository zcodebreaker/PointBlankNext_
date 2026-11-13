package com.pointblank.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
public class LegacyPasswordHasher {

    private static final byte[] HMAC_KEY =
            "/x!a@r-$r%an¨.&e&+f*f(f(a)".getBytes(StandardCharsets.UTF_8);

    /**
     * Gera o HMAC-MD5 hexadecimal esperado pelo servidor PointBlank 3.108.
     *
     * @param password senha em texto recebida pela conexão HTTPS
     * @return hash hexadecimal minúsculo de 32 caracteres
     */
    public String hash(String password) {
        try {
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(new SecretKeySpec(HMAC_KEY, "HmacMD5"));
            byte[] hash = mac.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("HmacMD5 não está disponível nesta JVM", exception);
        } catch (java.security.InvalidKeyException exception) {
            throw new IllegalStateException("A chave legada de senha é inválida", exception);
        }
    }
}
