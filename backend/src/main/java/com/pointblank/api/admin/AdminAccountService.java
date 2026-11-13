package com.pointblank.api.admin;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@Service
public class AdminAccountService {

    private static final int ADMIN_ACCESS_LEVEL = 5;

    private final AccountRepository accountRepository;
    private final AdminAccountDetailsRepository accountDetailsRepository;

    /**
     * Cria o serviço administrativo de contas.
     *
     * @param accountRepository repositório de contas
     */
    public AdminAccountService(
            AccountRepository accountRepository,
            AdminAccountDetailsRepository accountDetailsRepository) {
        this.accountRepository = accountRepository;
        this.accountDetailsRepository = accountDetailsRepository;
    }

    /**
     * Localiza uma única conta pelo identificador ou pelo nome de usuário exato.
     *
     * @param administratorId identificador do administrador
     * @param query Player ID ou nome de usuário
     * @return detalhes da conta encontrada
     */
    @Transactional(readOnly = true)
    public AdminAccountDetailsResponse findOne(long administratorId, String query) {
        requireAdministrator(administratorId);

        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            throw new AdminAccountException(HttpStatus.BAD_REQUEST, "Informe o Player ID ou o nome de usuário");
        }
        if (normalizedQuery.length() > 64) {
            throw new AdminAccountException(HttpStatus.BAD_REQUEST, "A busca informada é muito longa");
        }

        if (normalizedQuery.chars().allMatch(Character::isDigit)) {
            try {
                long playerId = Long.parseLong(normalizedQuery);
                if (playerId > 0) {
                    Optional<AdminAccountDetailsResponse> account = accountDetailsRepository.findByPlayerId(playerId);
                    if (account.isPresent()) return account.get();
                }
            } catch (NumberFormatException ignored) {
                // O valor ainda pode ser um nome de usuário composto apenas por números.
            }
        }

        return accountDetailsRepository.findByUsername(normalizedQuery)
                .orElseThrow(AdminAccountService::accountNotFound);
    }

    /**
     * Autoriza o administrador e lista contas com filtro e paginação.
     *
     * @param administratorId identificador do administrador
     * @param search filtro textual opcional
     * @param page índice da página
     * @param size quantidade máxima de itens
     * @return página de contas
     */
    @Transactional(readOnly = true)
    public AdminAccountPageResponse findAll(long administratorId, String search, int page, int size) {
        requireAdministrator(administratorId);

        long totalElements = accountRepository.countAll(search);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        List<AdminAccountResponse> accounts = accountRepository.findAll(search, page, size).stream()
                .map(AdminAccountResponse::from)
                .toList();

        return new AdminAccountPageResponse(accounts, page, size, totalElements, totalPages);
    }

    /**
     * Autoriza o administrador e atualiza as moedas de uma conta.
     *
     * @param administratorId identificador do administrador
     * @param playerId identificador da conta alvo
     * @param request novos saldos
     * @return conta atualizada
     */
    @Transactional
    public AdminAccountResponse updateCurrencies(
            long administratorId,
            long playerId,
            UpdateAccountCurrenciesRequest request) {
        requireAdministrator(administratorId);
        requireAccount(playerId);

        if (!accountRepository.updateGold(playerId, request.gold())) {
            throw accountNotFound();
        }

        if (!accountRepository.updateCash(playerId, request.cash())) {
            throw accountNotFound();
        }

        return AdminAccountResponse.from(requireAccount(playerId));
    }

    /**
     * Autoriza o administrador e soma moedas aos saldos atuais da conta.
     *
     * @param administratorId identificador do administrador
     * @param playerId identificador da conta alvo
     * @param request quantidades que serão adicionadas
     * @return conta atualizada
     */
    @Transactional
    public AdminAccountResponse addCurrencies(
            long administratorId,
            long playerId,
            AddAccountCurrenciesRequest request) {
        requireAdministrator(administratorId);
        requireAccount(playerId);

        if (request.gold() == 0 && request.cash() == 0) {
            throw new AdminAccountException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma quantidade de Gold ou Cash para adicionar");
        }

        if (!accountRepository.addCurrencies(playerId, request.gold(), request.cash())) {
            throw new AdminAccountException(
                    HttpStatus.CONFLICT,
                    "O saldo resultante excede o limite permitido");
        }

        return AdminAccountResponse.from(requireAccount(playerId));
    }

    /**
     * Autoriza o administrador e altera o banimento de uma conta comum.
     *
     * @param administratorId identificador do administrador
     * @param playerId identificador da conta alvo
     * @param request estado de banimento desejado
     * @return conta atualizada
     */
    @Transactional
    public AdminAccountResponse updateBanStatus(
            long administratorId,
            long playerId,
            UpdateAccountBanRequest request) {
        requireAdministrator(administratorId);
        Account target = requireAccount(playerId);

        if (target.accessLevel() >= ADMIN_ACCESS_LEVEL) {
            throw new AdminAccountException(
                    HttpStatus.CONFLICT,
                    "Contas administrativas não podem ser banidas ou desbanidas por este painel");
        }

        if (!accountRepository.updateBanStatus(playerId, request.banned())) {
            throw accountNotFound();
        }

        return AdminAccountResponse.from(requireAccount(playerId));
    }

    /**
     * Exige que a conta informada tenha nível de acesso administrativo.
     *
     * @param administratorId identificador da conta autenticada
     * @return conta administrativa validada
     */
    private Account requireAdministrator(long administratorId) {
        Account administrator = requireAccount(administratorId);
        if (administrator.accessLevel() < ADMIN_ACCESS_LEVEL) {
            throw new AdminAccountException(HttpStatus.FORBIDDEN, "Acesso administrativo necessário");
        }
        return administrator;
    }

    /**
     * Busca uma conta ou interrompe a operação quando ela não existe.
     *
     * @param playerId identificador da conta
     * @return conta encontrada
     */
    private Account requireAccount(long playerId) {
        return accountRepository.findByPlayerId(playerId)
                .orElseThrow(AdminAccountService::accountNotFound);
    }

    /**
     * Cria a exceção padronizada para uma conta inexistente.
     *
     * @return exceção administrativa com status 404
     */
    private static AdminAccountException accountNotFound() {
        return new AdminAccountException(HttpStatus.NOT_FOUND, "Conta não encontrada");
    }
}
