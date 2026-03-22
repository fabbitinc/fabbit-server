package com.fabbitinc.server.application.chat.support;

import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatInputGuard {

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions?"),
            Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+)?prompt"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+"),
            Pattern.compile("(?i)developer\\s+mode"),
            Pattern.compile("(?i)DAN\\s+mode"),
            Pattern.compile("(?i)bypass\\s+(all\\s+)?restrictions?"),
            Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)"),
            Pattern.compile("(?i)act\\s+as\\s+if\\s+you\\s+have\\s+no\\s+restrictions"),
            Pattern.compile("(?i)override\\s+(previous|safety)\\s+"),
            Pattern.compile("이전\\s+지시(사항)?\\s*무시"),
            Pattern.compile("시스템\\s*프롬프트\\s*(공개|알려|보여)"),
            Pattern.compile("(?i)system\\s*prompt")
    );

    private static final Pattern ZERO_WIDTH_CHARS = Pattern.compile("[\\u200b\\u200c\\u200d\\ufeff]");

    public GuardResult check(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return GuardResult.safe();
        }

        String normalized = normalize(userInput);

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(normalized).find()) {
                log.warn("event=chat_input_injection_detected pattern={}", pattern.pattern());
                return GuardResult.blocked("injection_detected");
            }
        }

        return GuardResult.safe();
    }

    private String normalize(String input) {
        String normalized = ZERO_WIDTH_CHARS.matcher(input).replaceAll("");
        return normalized.strip();
    }

    public record GuardResult(
            boolean blocked,
            String reason
    ) {
        public static GuardResult safe() {
            return new GuardResult(false, null);
        }

        public static GuardResult blocked(String reason) {
            return new GuardResult(true, reason);
        }
    }
}
