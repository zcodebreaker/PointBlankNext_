package com.pointblank.api.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.pointblank.api.config.AppProperties;

@Configuration
public class SecurityConfig {

    /**
     * Configura autorização, CORS, JWT e política stateless da API.
     *
     * @param http construtor da segurança HTTP
     * @return cadeia de filtros configurada
     * @throws Exception quando a configuração não puder ser construída
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/auth/username-availability",
                                "/api/v1/auth/email-availability",
                                "/api/v1/auth/verify-email",
                                "/api/v1/rankings/clans",
                                "/api/v1/rankings/players",
                                "/api/v1/downloads/client",
                                "/api/v1/webstore/items",
                                "/api/v1/webstore/items/*/cover",
                                "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAuthority("SCOPE_player")
                        .requestMatchers("/api/v1/accounts/**").hasAuthority("SCOPE_player")
                        .requestMatchers("/api/v1/tickets/**").hasAuthority("SCOPE_player")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    /**
     * Constrói a chave simétrica usada para assinar e validar tokens.
     *
     * @param properties configurações que contêm o segredo da aplicação
     * @return chave HMAC-SHA256
     */
    @Bean
    SecretKey jwtSecretKey(AppProperties properties) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET deve possuir no mínimo 32 bytes");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /**
     * Cria o codificador de JWT baseado na chave da aplicação.
     *
     * @param secretKey chave de assinatura
     * @return codificador JWT
     */
    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Cria o decodificador e valida o emissor configurado dos tokens.
     *
     * @param secretKey chave usada para verificar a assinatura
     * @param properties configurações que definem o emissor esperado
     * @return decodificador JWT
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey, AppProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.jwt().issuer()));
        return decoder;
    }

    /**
     * Define origens, métodos e cabeçalhos aceitos pelo CORS.
     *
     * @param properties configurações com as origens permitidas
     * @return fonte de configuração CORS para todas as rotas
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
        List<String> allowedOrigins = Arrays.stream(properties.cors().allowedOrigins().split(","))
                .map(origin -> origin.trim())
                .filter(origin -> !origin.isEmpty())
                .toList();

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
