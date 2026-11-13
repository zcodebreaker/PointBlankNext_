package com.pointblank.api.security;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.pointblank.api.account.Account;
import com.pointblank.api.config.AppProperties;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties properties;

    /**
     * Cria o serviço emissor de JWT.
     *
     * @param jwtEncoder codificador dos tokens
     * @param properties configurações de autenticação da aplicação
     */
    public JwtService(JwtEncoder jwtEncoder, AppProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * Emite um token assinado para a conta e duração solicitadas.
     *
     * @param account conta autenticada
     * @param remember indica se deve ser usada a duração estendida
     * @return token assinado e seu tempo de validade em segundos
     */
    public IssuedToken issue(Account account, boolean remember) {
        Instant issuedAt = Instant.now();
        Duration timeToLive = remember
                ? properties.jwt().rememberTokenTtl()
                : properties.jwt().accessTokenTtl();
        Instant expiresAt = issuedAt.plus(timeToLive);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(Long.toString(account.playerId()))
                .claim("username", account.username())
                .claim("scope", "player")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, timeToLive.toSeconds());
    }

    public record IssuedToken(String value, long expiresIn) {
    }
}
