package rakib.bcs430healthcareproject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class MessageCensor {

    private static final Set<String> BLOCKED_WORDS = loadBlockedWords();

    public static String censor(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String censored = message;

        for (String blockedWord : BLOCKED_WORDS) {
            String regex = buildFlexibleRegex(blockedWord);

            censored = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    .matcher(censored)
                    .replaceAll(match -> "*".repeat(Math.max(4, match.group().length())));
        }

        return censored;
    }

    public static boolean containsBlockedWords(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        for (String blockedWord : BLOCKED_WORDS) {
            String regex = buildFlexibleRegex(blockedWord);

            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(message).find()) {
                return true;
            }
        }

        return false;
    }

    private static Set<String> loadBlockedWords() {
        Set<String> words = new LinkedHashSet<>();

        try {
            InputStream stream = MessageCensor.class.getResourceAsStream(
                    "/rakib/bcs430healthcareproject/blocked_words.txt"

            );
/**
 * List from https://github.com/dsojevic/profanity-list
 */

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            )) {
                String line;

                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();

                    if (!line.isBlank() && !line.startsWith("#")) {
                        words.add(line);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return words;
    }

    private static String buildFlexibleRegex(String word) {
        String normalized = word.toLowerCase().trim();

        StringBuilder regex = new StringBuilder();
        regex.append("(?<![a-zA-Z0-9])");

        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);

            if (c == ' ') {
                regex.append("[\\W_]*");
                continue;
            }

            regex.append(charPattern(c));
            regex.append("[\\W_]*");
        }

        regex.append("(?![a-zA-Z0-9])");

        return regex.toString();
    }

    private static String charPattern(char c) {
        return switch (c) {
            case 'a' -> "[aA@4]";
            case 'b' -> "[bB8]";
            case 'e' -> "[eE3]";
            case 'i' -> "[iI1!|]";
            case 'l' -> "[lL1|!]";
            case 'o' -> "[oO0]";
            case 's' -> "[sS5$]";
            case 't' -> "[tT7]";
            case 'u' -> "[uUvV]";
            default -> Pattern.quote(String.valueOf(c));
        };
    }
}