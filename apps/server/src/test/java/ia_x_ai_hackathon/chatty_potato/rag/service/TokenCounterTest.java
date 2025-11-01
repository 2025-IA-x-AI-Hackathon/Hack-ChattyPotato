package ia_x_ai_hackathon.chatty_potato.rag.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenCounter 유틸리티 테스트")
class TokenCounterTest {

    @Test
    @DisplayName("null은 0 토큰")
    void null_returns_zero() {
        assertThat(TokenCounter.count(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("빈 문자열은 0 토큰")
    void empty_returns_zero() {
        assertThat(TokenCounter.count("")).isEqualTo(0);
        assertThat(TokenCounter.count("   ")).isEqualTo(0);
    }

    @ParameterizedTest
    @CsvSource({
            "'Hello',                               1,  2",
            "'Hello world',                         2,  4",
            "'The quick brown fox',                 4,  8",
            "'안녕하세요',                            1,  2",
            "'안녕하세요 반갑습니다',                   2,  4",
            "'Hello world 안녕하세요',               3,  6",
    })
    @DisplayName("단어 수 × 2 = 토큰 수")
    void word_count_times_two(String text, int wordCount, int expectedTokens) {
        assertThat(TokenCounter.count(text)).isEqualTo(expectedTokens);
    }

    @Test
    @DisplayName("연속 공백 처리")
    void handles_multiple_spaces() {
        assertThat(TokenCounter.count("Hello    world")).isEqualTo(4);
        assertThat(TokenCounter.count("  Hello   world  ")).isEqualTo(4);
    }

    @Test
    @DisplayName("countAll은 여러 텍스트 합산")
    void countAll_sums_texts() {
        int total = TokenCounter.countAll(
                "Hello world",      // 4
                "안녕하세요",         // 2
                "Good morning"      // 4
        );
        assertThat(total).isEqualTo(10);
    }

    @Test
    @DisplayName("실제 케이스: 긴 질문")
    void real_world_long_question() {
        String query = "우리 회사의 2024년 Q3 매출이 전년 대비 감소한 이유를 분석하고 개선 전략을 제시해주세요";

        // 띄어쓰기로 세면 13단어
        // 13 × 2 = 28 토큰
        assertThat(TokenCounter.count(query)).isEqualTo(26);
    }

    @Test
    @DisplayName("실제 케이스: 짧은 질문")
    void real_world_short_question() {
        String query = "Python Hello World 코드";

        // 4단어 × 2 = 8 토큰
        assertThat(TokenCounter.count(query)).isEqualTo(8);
    }
}

