package ia_x_ai_hackathon.chatty_potato.rag.service;

import org.springframework.stereotype.Component;

/**
 * 토큰 수 추정 유틸리티
 * - 추정 방식: 띄어쓰기 기준 단어 수 × 2
 * - 보수적 추정으로 토큰 초과 방지
 */
public final class TokenCounter {

    private static final int MULTIPLIER = 2;

    private TokenCounter() {
        throw new AssertionError("Utility class");
    }

    /**
     * 텍스트의 근사 토큰 수 계산
     *
     * @param text 입력 텍스트
     * @return 추정 토큰 수 (단어 수 × 2)
     */
    public static int count(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 0;
        }

        // 연속 공백을 하나로 처리하고 단어 개수 계산
        String[] words = trimmed.split("\\s+");
        return words.length * MULTIPLIER;
    }

    /**
     * 여러 텍스트의 총 토큰 수 계산
     */
    public static int countAll(String... texts) {
        int total = 0;
        for (String text : texts) {
            total += count(text);
        }
        return total;
    }
}