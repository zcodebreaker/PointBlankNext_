package com.pointblank.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.pointblank.api.config.AppProperties;

class VerificationEmailServiceTests {

    @Test
    void sendsSiteThemedHtmlEmailWithPlainTextFallbackAndEmbeddedAssets() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage message = new JavaMailSenderImpl().createMimeMessage();
        when(mailSender.createMimeMessage()).thenReturn(message);

        AppProperties properties = new AppProperties(
                new AppProperties.Jwt("01234567890123456789012345678901", "pointblank-api", Duration.ofHours(2), Duration.ofDays(30)),
                new AppProperties.Cors("http://localhost:3000"),
                new AppProperties.EmailVerification("http://localhost:3000/", "noreply@kryonixnetwork.com", Duration.ofHours(24)));
        VerificationEmailService service = new VerificationEmailService(mailSender, properties);

        service.sendActivation("player@example.com", "player<&", "activation-token");

        verify(mailSender).send(message);
        message.saveChanges();
        assertThat(message.getSubject()).isEqualTo("Ative sua conta — Kryonix Network");
        assertThat(message.getFrom()[0].toString()).isEqualTo("noreply@kryonixnetwork.com");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo("player@example.com");

        List<Part> parts = flatten(message);
        String plainText = contentOf(parts, "text/plain");
        String html = contentOf(parts, "text/html");

        assertThat(plainText)
                .contains("Olá, player<&!")
                .contains("http://localhost:3000/member/verify-email?token=activation-token")
                .contains("24 horas");
        assertThat(html)
                .contains("player&lt;&amp;")
                .contains("Ativar minha conta")
                .contains("cid:kryonix-logo")
                .contains("cid:pointblank-logo")
                .contains("cid:verification-background")
                .contains("cid:corner-top-left")
                .contains("cid:corner-bottom-right")
                .contains("background-color:#041428")
                .contains("background-color:#080808")
                .contains("border:1px solid #ff0000")
                .contains("http://localhost:3000/member/verify-email?token=activation-token")
                .contains("http://localhost:3000/#rodape")
                .contains("Copyright Kryonix Network. Todos direitos reservados.");

        assertThat(parts.stream().filter(part -> contentId(part) != null).map(VerificationEmailServiceTests::contentId))
                .containsExactlyInAnyOrder(
                        "<kryonix-logo>",
                        "<pointblank-logo>",
                        "<verification-background>",
                        "<corner-top-left>",
                        "<corner-bottom-right>");
    }

    private static List<Part> flatten(Part root) throws Exception {
        List<Part> parts = new ArrayList<>();
        collectParts(root, parts);
        return parts;
    }

    private static void collectParts(Part part, List<Part> parts) throws Exception {
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int index = 0; index < multipart.getCount(); index++) {
                BodyPart bodyPart = multipart.getBodyPart(index);
                collectParts(bodyPart, parts);
            }
            return;
        }
        parts.add(part);
    }

    private static String contentOf(List<Part> parts, String mimeType) throws Exception {
        for (Part part : parts) {
            if (part.isMimeType(mimeType)) {
                return (String) part.getContent();
            }
        }
        throw new AssertionError("Parte MIME ausente: " + mimeType);
    }

    private static String contentId(Part part) {
        try {
            String[] header = part.getHeader("Content-ID");
            return header == null || header.length == 0 ? null : header[0];
        } catch (Exception exception) {
            throw new AssertionError("Não foi possível ler o Content-ID", exception);
        }
    }
}
