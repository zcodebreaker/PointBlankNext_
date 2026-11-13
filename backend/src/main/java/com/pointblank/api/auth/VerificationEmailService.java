package com.pointblank.api.auth;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.pointblank.api.config.AppProperties;

@Service
public class VerificationEmailService {

    private static final String KRYONIX_LOGO_CONTENT_ID = "kryonix-logo";
    private static final String POINT_BLANK_LOGO_CONTENT_ID = "pointblank-logo";
    private static final String BACKGROUND_CONTENT_ID = "verification-background";
    private static final String CORNER_TOP_LEFT_CONTENT_ID = "corner-top-left";
    private static final String CORNER_BOTTOM_RIGHT_CONTENT_ID = "corner-bottom-right";
    private static final ClassPathResource KRYONIX_LOGO_RESOURCE =
            new ClassPathResource("mail/logo_kryonix_ntw.png");
    private static final ClassPathResource POINT_BLANK_LOGO_RESOURCE =
            new ClassPathResource("mail/logo_bi_wide.png");
    private static final ClassPathResource BACKGROUND_RESOURCE =
            new ClassPathResource("mail/verification-background.jpg");
    private static final ClassPathResource CORNER_TOP_LEFT_RESOURCE =
            new ClassPathResource("mail/icon_lt.png");
    private static final ClassPathResource CORNER_BOTTOM_RIGHT_RESOURCE =
            new ClassPathResource("mail/icon_rb.png");

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public VerificationEmailService(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void sendActivation(String recipient, String username, String token) {
        String publicUrl = properties.emailVerification().publicUrl().replaceAll("/+$", "");
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String activationUrl = publicUrl + "/member/verify-email?token=" + encodedToken;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(properties.emailVerification().fromAddress());
            helper.setTo(recipient);
            helper.setSubject("Ative sua conta — Kryonix Network");
            helper.setText(
                    buildPlainText(username, activationUrl),
                    buildHtml(username, activationUrl, publicUrl + "/#rodape"));
            helper.addInline(KRYONIX_LOGO_CONTENT_ID, KRYONIX_LOGO_RESOURCE, "image/png");
            helper.addInline(POINT_BLANK_LOGO_CONTENT_ID, POINT_BLANK_LOGO_RESOURCE, "image/png");
            helper.addInline(BACKGROUND_CONTENT_ID, BACKGROUND_RESOURCE, "image/jpeg");
            helper.addInline(CORNER_TOP_LEFT_CONTENT_ID, CORNER_TOP_LEFT_RESOURCE, "image/png");
            helper.addInline(CORNER_BOTTOM_RIGHT_CONTENT_ID, CORNER_BOTTOM_RIGHT_RESOURCE, "image/png");

            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new EmailDeliveryException(exception);
        }
    }

    private String buildPlainText(String username, String activationUrl) {
        return """
                Olá, %s!

                Confirme seu e-mail para ativar sua conta do Point Blank:
                %s

                Este link expira em %s. Se você não fez este cadastro, ignore esta mensagem.

                COPYRIGHT KRYONIX NETWORK. TODOS DIREITOS RESERVADOS.
                """.formatted(username, activationUrl, formatTtl());
    }

    private String buildHtml(String username, String activationUrl, String supportUrl) {
        String safeUsername = escapeHtml(username);
        String safeActivationUrl = escapeHtml(activationUrl);
        String safeSupportUrl = escapeHtml(supportUrl);
        String safeTtl = escapeHtml(formatTtl());

        return """
                <!doctype html>
                <html lang="pt-BR">
                  <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Ative sua conta — Kryonix Network</title>
                  </head>
                  <body style="margin:0;padding:0;background-color:#041428;font-family:Arial,Helvetica,sans-serif;color:#ffffff;">
                    <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
                      Confirme seu e-mail para ativar sua conta do Point Blank.
                    </div>
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#041428" style="width:100%%;background-color:#041428;">
                      <tr>
                        <td align="center" style="padding:0 10px;">
                          <table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:600px;border-collapse:collapse;">
                            <tr>
                              <td background="cid:%s" align="center" valign="top" bgcolor="#16113b" style="padding:48px 20px 44px;background-color:#16113b;background-image:url('cid:%s');background-position:center top;background-repeat:no-repeat;background-size:cover;">
                                <img src="cid:%s" width="352" height="107" alt="Point Blank Beyond Limits" style="display:block;width:100%%;max-width:352px;height:auto;border:0;outline:none;text-decoration:none;">

                                <table role="presentation" width="520" cellspacing="0" cellpadding="0" border="0" bgcolor="#080808" style="width:100%%;max-width:520px;margin-top:42px;background-color:#080808;border:1px solid #3a3a3a;border-collapse:collapse;">
                                  <tr>
                                    <td width="18" height="18" align="left" valign="top" style="width:18px;height:18px;font-size:0;line-height:0;">
                                      <img src="cid:%s" width="18" height="18" alt="" style="display:block;width:18px;height:18px;border:0;">
                                    </td>
                                    <td style="font-size:0;line-height:0;">&nbsp;</td>
                                  </tr>
                                  <tr>
                                    <td colspan="2" align="center" style="padding:22px 48px 42px;">
                                      <p style="margin:0;color:#87cfff;font-size:13px;font-weight:bold;line-height:18px;letter-spacing:2px;text-transform:uppercase;">
                                        Confirmação de e-mail
                                      </p>
                                      <h1 style="margin:14px 0 16px;color:#ffffff;font-size:28px;line-height:35px;text-transform:uppercase;">
                                        Confirme seu e-mail
                                      </h1>
                                      <p style="margin:0 0 12px;color:#ffffff;font-size:15px;line-height:24px;">
                                        Olá, <strong>%s</strong>!
                                      </p>
                                      <p style="margin:0;color:#d9eaf5;font-size:14px;line-height:24px;">
                                        Seu cadastro foi recebido. Clique no botão abaixo para confirmar seu e-mail e ativar sua conta.
                                      </p>

                                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-top:28px;border-collapse:collapse;">
                                        <tr>
                                          <td style="height:1px;background-color:#467294;font-size:0;line-height:0;">&nbsp;</td>
                                          <td width="10" height="3" bgcolor="#ff0000" style="width:10px;height:3px;background-color:#ff0000;font-size:0;line-height:0;">&nbsp;</td>
                                          <td style="height:1px;background-color:#467294;font-size:0;line-height:0;">&nbsp;</td>
                                        </tr>
                                      </table>

                                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-top:26px;border-collapse:separate;">
                                        <tr>
                                          <td align="center" bgcolor="#990000" style="border:1px solid #ff0000;border-radius:5px;box-shadow:0 3px 5px rgba(0,0,0,0.7);">
                                            <a href="%s" target="_blank" style="display:block;padding:19px 20px;color:#ffffff;font-size:15px;font-weight:bold;line-height:20px;letter-spacing:0.2px;text-align:center;text-decoration:none;text-transform:uppercase;">
                                              Ativar minha conta
                                            </a>
                                          </td>
                                        </tr>
                                      </table>

                                      <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" bgcolor="#1b3446" style="margin-top:24px;background-color:#1b3446;border:1px solid #87cfff;border-collapse:collapse;">
                                        <tr>
                                          <td style="padding:14px 16px;color:#d9eaf5;font-size:12px;line-height:19px;text-align:left;">
                                            Este link expira em <strong style="color:#ffffff;">%s</strong> e só pode ser usado uma vez.
                                          </td>
                                        </tr>
                                      </table>

                                      <p style="margin:23px 0 7px;color:#87cfff;font-size:11px;line-height:17px;text-align:left;">
                                        Se o botão não abrir, copie e cole este endereço no navegador:
                                      </p>
                                      <p style="margin:0;word-break:break-all;color:#87cfff;font-size:11px;line-height:17px;text-align:left;">
                                        <a href="%s" target="_blank" style="color:#87cfff;text-decoration:underline;">%s</a>
                                      </p>
                                      <p style="margin:22px 0 0;color:#8da0ae;font-size:11px;line-height:18px;">
                                        Se você não fez este cadastro, ignore esta mensagem.
                                      </p>
                                    </td>
                                  </tr>
                                  <tr>
                                    <td style="font-size:0;line-height:0;">&nbsp;</td>
                                    <td height="18" align="right" valign="bottom" style="height:18px;font-size:0;line-height:0;">
                                      <img src="cid:%s" width="18" height="18" alt="" style="display:block;width:18px;height:18px;margin-left:auto;border:0;">
                                    </td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                            <tr>
                              <td align="center" bgcolor="#041428" style="padding:32px 24px 38px;background-color:#041428;">
                                <img src="cid:%s" width="160" height="58" alt="Kryonix Network" style="display:block;width:160px;height:58px;margin:0 auto;border:0;outline:none;text-decoration:none;">
                                <p style="margin:22px 0 8px;color:#e5e5e5;font-size:11px;line-height:17px;text-transform:uppercase;">
                                  Se houver dúvidas, entre em contato com o <a href="%s" target="_blank" style="color:#e72427;text-decoration:none;">suporte</a>.
                                </p>
                                <p style="margin:0;color:#e5e5e5;font-size:10px;line-height:16px;letter-spacing:0.1px;text-transform:uppercase;">
                                  Copyright Kryonix Network. Todos direitos reservados.
                                </p>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(
                BACKGROUND_CONTENT_ID,
                BACKGROUND_CONTENT_ID,
                POINT_BLANK_LOGO_CONTENT_ID,
                CORNER_TOP_LEFT_CONTENT_ID,
                safeUsername,
                safeActivationUrl,
                safeTtl,
                safeActivationUrl,
                safeActivationUrl,
                CORNER_BOTTOM_RIGHT_CONTENT_ID,
                KRYONIX_LOGO_CONTENT_ID,
                safeSupportUrl);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatTtl() {
        long hours = properties.emailVerification().tokenTtl().toHours();
        if (hours > 0) {
            return hours == 1 ? "1 hora" : hours + " horas";
        }
        long minutes = Math.max(1, properties.emailVerification().tokenTtl().toMinutes());
        return minutes == 1 ? "1 minuto" : minutes + " minutos";
    }
}
