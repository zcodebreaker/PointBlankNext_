package com.pointblank.api.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * Cria o controlador de autenticação.
     *
     * @param authService serviço que executa cadastro, login e consultas de disponibilidade
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Cadastra uma nova conta e envia o link de ativação.
     *
     * @param request dados enviados pelo formulário de cadastro
     * @return instrução para confirmar o cadastro
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistrationResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * Ativa uma conta por meio do token enviado ao e-mail cadastrado.
     *
     * @param token token recebido por e-mail
     * @return resultado da confirmação
     */
    @GetMapping("/verify-email")
    public EmailVerificationResponse verifyEmail(@RequestParam String token) {
        return authService.verifyEmail(token);
    }

    /**
     * Autentica uma conta existente.
     *
     * @param request credenciais e preferência de persistência da sessão
     * @return token e dados públicos da conta autenticada
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Consulta a disponibilidade de um nome de usuário.
     *
     * @param username nome de usuário candidato
     * @return nome normalizado e indicação de disponibilidade
     */
    @GetMapping("/username-availability")
    public UsernameAvailabilityResponse usernameAvailability(@RequestParam String username) {
        return authService.usernameAvailability(username);
    }

    /**
     * Consulta a disponibilidade de um e-mail.
     *
     * @param email e-mail candidato
     * @return e-mail normalizado e indicação de disponibilidade
     */
    @GetMapping("/email-availability")
    public EmailAvailabilityResponse emailAvailability(@RequestParam String email) {
        return authService.emailAvailability(email);
    }
}
