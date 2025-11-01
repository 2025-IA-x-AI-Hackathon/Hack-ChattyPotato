package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ia_x_ai_hackathon.chatty_potato.rag.dto.RewriteResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RewriteChainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

@ExtendWith(MockitoExtension.class)
@DisplayName("RewriteService 테스트")
class RewriteChainServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private RewriteChainService rewriteChainService;

    @BeforeEach
    void setUp() {
        rewriteChainService = new RewriteChainService(chatClient);
    }

    @Test
    @DisplayName("정상 쿼리는 LLM을 통해 재작성됨")
    void normal_query_gets_rewritten() {
        // given
        String originalQuery = "요즘 인기 있는 AI 기술이 뭐야?";
        String rewrittenQuery = "최신 인기 AI 기술 트렌드";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(rewrittenQuery);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(originalQuery);

        // then
        assertThat(result.originalQuery()).isEqualTo(originalQuery);
        assertThat(result.rewrittenQuery()).isEqualTo(rewrittenQuery);
        assertThat(result.originalTokens()).isGreaterThan(0);
        assertThat(result.rewrittenTokens()).isGreaterThan(0);

        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    @DisplayName("모호한 쿼리를 명확하게 재작성")
    void vague_query_becomes_specific() {
        // given
        String vague = "저거 어떻게 하는지 알려줘";
        String specific = "프로그래밍 방법 설명";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(specific);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(vague);

        // then
        assertThat(result.rewrittenQuery()).isEqualTo(specific);
        assertThat(result.rewrittenQuery()).isNotEqualTo(result.originalQuery());
    }

    @Test
    @DisplayName("빈 문자열 입력 시 빈 문자열 반환")
    void empty_query_returns_empty() {
        // given
        String emptyQuery = "";

        // when
        RewriteResultDto result = rewriteChainService.rewrite(emptyQuery);

        // then
        assertThat(result.originalQuery()).isEmpty();
        assertThat(result.rewrittenQuery()).isEmpty();
        assertThat(result.originalTokens()).isEqualTo(0);
        assertThat(result.rewrittenTokens()).isEqualTo(0);

        // LLM 호출 안 함
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    @DisplayName("null 입력 시 null 반환")
    void null_query_returns_null() {
        // given
        String nullQuery = null;

        // when
        RewriteResultDto result = rewriteChainService.rewrite(nullQuery);

        // then
        assertThat(result.originalQuery()).isNull();
        assertThat(result.rewrittenQuery()).isNull();

        // LLM 호출 안 함
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    @DisplayName("공백만 있는 쿼리는 처리하지 않음")
    void whitespace_query_not_processed() {
        // given
        String whitespaceQuery = "   \n\t   ";

        // when
        RewriteResultDto result = rewriteChainService.rewrite(whitespaceQuery);

        // then
        assertThat(result.rewrittenQuery()).isEqualTo(whitespaceQuery);

        // LLM 호출 안 함
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    @DisplayName("LLM 재작성 실패 시 원본 쿼리 반환")
    void llm_failure_returns_original() {
        // given
        String query = "테스트 쿼리";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenThrow(new RuntimeException("LLM API Error"));

        // when
        RewriteResultDto result = rewriteChainService.rewrite(query);

        // then
        assertThat(result.originalQuery()).isEqualTo(query);
        assertThat(result.rewrittenQuery()).isEqualTo(query);

        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    @DisplayName("LLM이 빈 응답 반환 시 원본 쿼리 사용")
    void llm_empty_response_uses_original() {
        // given
        String query = "테스트 쿼리";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("");

        // when
        RewriteResultDto result = rewriteChainService.rewrite(query);

        // then
        assertThat(result.originalQuery()).isEqualTo(query);
        assertThat(result.rewrittenQuery()).isEqualTo(query);
    }

    @Test
    @DisplayName("LLM이 공백만 반환 시 원본 쿼리 사용")
    void llm_whitespace_response_uses_original() {
        // given
        String query = "테스트 쿼리";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("   \n\t   ");

        // when
        RewriteResultDto result = rewriteChainService.rewrite(query);

        // then
        assertThat(result.originalQuery()).isEqualTo(query);
        assertThat(result.rewrittenQuery()).isEqualTo(query);
    }

    @Test
    @DisplayName("긴 쿼리도 정상 처리")
    void handles_long_query() {
        // given
        String longQuery = "AI 기술에 대해 자세히 알려주세요. ".repeat(20);
        String rewritten = "AI 기술 상세 설명";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(rewritten);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(longQuery);

        // then
        assertThat(result.originalQuery()).isEqualTo(longQuery);
        assertThat(result.rewrittenQuery()).isEqualTo(rewritten);
        assertThat(result.rewrittenTokens()).isLessThan(result.originalTokens());
    }

    @Test
    @DisplayName("특수문자 포함 쿼리 처리")
    void handles_special_characters() {
        // given
        String queryWithSpecialChars = "C++ vs Java: 어떤게 나아?! #개발";
        String rewritten = "C++ Java 비교 분석";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(rewritten);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(queryWithSpecialChars);

        // then
        assertThat(result.rewrittenQuery()).isEqualTo(rewritten);
    }

    @Test
    @DisplayName("실제 사용 시나리오: 대화형 쿼리를 검색 쿼리로 변환")
    void realistic_conversational_to_search_query() {
        // given
        String conversational = "저 어제 본 그 영화 제목이 뭐였더라? 로봇 나오는 거";
        String searchOptimized = "로봇 영화 제목";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(searchOptimized);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(conversational);

        // then
        assertThat(result.originalQuery()).isEqualTo(conversational);
        assertThat(result.rewrittenQuery()).isEqualTo(searchOptimized);
        assertThat(result.rewrittenQuery().length()).isLessThan(result.originalQuery().length());

        // 토큰 수 검증
        assertThat(result.originalTokens()).isGreaterThan(0);
        assertThat(result.rewrittenTokens()).isGreaterThan(0);
        assertThat(result.rewrittenTokens()).isLessThanOrEqualTo(result.originalTokens());

        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    @DisplayName("재작성 쿼리는 100 토큰 이하로 제한")
    void rewritten_query_stays_under_token_limit() {
        // given: 매우 긴 원본 쿼리
        String longQuery = """
            안녕하세요, 저는 최근에 인공지능 기술에 대해서 관심이 생겨서 공부를 시작하려고 하는데요,
            특히 자연어 처리 분야와 컴퓨터 비전 분야 중에서 어떤 것을 먼저 시작하면 좋을지,
            그리고 각 분야에서 필요한 수학적 배경 지식은 무엇이 있는지,
            추천해주실 만한 온라인 강의나 책이 있다면 알려주시면 감사하겠습니다.
            """;

        // LLM이 간결하게 재작성 (100 토큰 이하)
        String conciseRewrite = "AI 자연어처리 vs 컴퓨터비전 학습 순서 수학 배경지식 추천 강의";

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(conciseRewrite);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(longQuery);

        // then
        assertThat(result.rewrittenTokens()).isLessThanOrEqualTo(100);
        assertThat(result.rewrittenTokens()).isLessThan(result.originalTokens());

        System.out.println("Original tokens: " + result.originalTokens());
        System.out.println("Rewritten tokens: " + result.rewrittenTokens());
    }

    @Test
    @DisplayName("토큰 제한 초과 시 경고 로그 발생")
    void warns_when_exceeding_token_limit() {
        // given: LLM이 너무 긴 재작성 결과 반환 (실수)
        String query = "AI 기술 추천";
        String tooLongRewrite = "인공지능 기술 추천 " + "매우 긴 설명 ".repeat(50); // 150+ 토큰

        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(tooLongRewrite);

        // when
        RewriteResultDto result = rewriteChainService.rewrite(query);

        // then
        assertThat(result.rewrittenTokens()).isGreaterThan(100);
        // 로그 확인은 실제로는 LogCaptor 등을 사용해야 하지만,
        // 여기서는 토큰 수만 검증
    }
}