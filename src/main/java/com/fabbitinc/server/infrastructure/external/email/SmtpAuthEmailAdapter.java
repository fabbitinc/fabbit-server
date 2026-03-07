package com.fabbitinc.server.infrastructure.external.email;

import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmtpAuthEmailAdapter implements AuthEmailPort {

    private static final String TEMPLATE_ROOT = "classpath:email_templates/";
    private static final String VERIFICATION_SUBJECT = "[Fabbit] 이메일 인증코드";

    private final AppProperties appProperties;
    private final ResourceLoader resourceLoader;
    private final JavaMailSender mailSender;

    public SmtpAuthEmailAdapter(AppProperties appProperties, ResourceLoader resourceLoader) {
        this.appProperties = appProperties;
        this.resourceLoader = resourceLoader;
        this.mailSender = createMailSender(appProperties);
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        String html = loadTemplate("verification.html").replace("{code}", code);
        String text = loadTemplate("verification.txt").replace("{code}", code);
        send(email, VERIFICATION_SUBJECT, html, text);
    }

    @Override
    public void sendInvitation(String email, String orgName, String inviterName, String inviteUrl) {
        String html = loadTemplate("invitation.html")
                .replace("{org_name}", orgName)
                .replace("{inviter_name}", inviterName)
                .replace("{invite_url}", inviteUrl);
        String text = loadTemplate("invitation.txt")
                .replace("{org_name}", orgName)
                .replace("{inviter_name}", inviterName)
                .replace("{invite_url}", inviteUrl);
        send(email, "[Fabbit] " + orgName + " 워크스페이스에 초대되었습니다", html, text);
    }

    private void send(String email, String subject, String htmlBody, String textBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setFrom(new InternetAddress(
                    appProperties.smtpFromEmail(),
                    appProperties.smtpFromName(),
                    StandardCharsets.UTF_8.name()
            ));
            helper.setText(textBody, htmlBody);
            mailSender.send(message);
            log.info("email sent: to={}, subject={}", email, subject);
        } catch (MessagingException | MailException | IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "이메일 발송 중 오류가 발생했습니다");
        }
    }

    private String loadTemplate(String fileName) {
        Resource resource = resourceLoader.getResource(TEMPLATE_ROOT + fileName);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "이메일 템플릿 로딩 중 오류가 발생했습니다");
        }
    }

    private JavaMailSender createMailSender(AppProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.smtpHost());
        sender.setPort(properties.smtpPort());
        sender.setUsername(properties.smtpUsername());
        sender.setPassword(properties.smtpPassword());
        sender.setProtocol("smtp");
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties mailProperties = sender.getJavaMailProperties();
        mailProperties.setProperty("mail.smtp.auth", Boolean.toString(!properties.smtpUsername().isBlank()));
        mailProperties.setProperty("mail.smtp.starttls.enable", Boolean.toString(properties.smtpUseTls()));
        mailProperties.setProperty("mail.smtp.connectiontimeout", "5000");
        mailProperties.setProperty("mail.smtp.timeout", "5000");
        mailProperties.setProperty("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
