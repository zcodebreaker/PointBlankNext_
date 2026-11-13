package com.pointblank.api.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.pointblank.api.account.Account;
import com.pointblank.api.account.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AdminTicketServiceTests {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private AccountRepository accountRepository;

    private AdminTicketService adminTicketService;

    @BeforeEach
    void setUp() {
        adminTicketService = new AdminTicketService(ticketRepository, ticketService, accountRepository);
    }

    @Test
    void rejectsRepliesToClosedTickets() {
        SupportTicket closedTicket = ticket(TicketStatus.CLOSED);
        when(accountRepository.findByPlayerId(5L)).thenReturn(Optional.of(administrator()));
        when(ticketService.requireTicket(12L)).thenReturn(closedTicket);

        assertThatThrownBy(() -> adminTicketService.reply(
                5L,
                12L,
                new AdminTicketReplyRequest("Assunto válido", "Nova resposta", false)))
                .isInstanceOfSatisfying(TicketException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).isEqualTo("Ticket encerrado não aceita novas respostas");
                });

        verify(ticketRepository, never()).reply(anyLong(), anyLong(), anyString(), anyString(), anyBoolean());
    }

    private static Account administrator() {
        return new Account(5L, "gm", "gm@example.com", "GM", 0, 0, 0, 0, 5, false);
    }

    private static SupportTicket ticket(TicketStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new SupportTicket(
                12L,
                38L,
                "player",
                "Atendimento Geral",
                "Outros",
                "Assunto original",
                "Mensagem original",
                status,
                "Assunto da resposta",
                "Resposta anterior",
                "gm",
                now,
                now,
                now);
    }
}
