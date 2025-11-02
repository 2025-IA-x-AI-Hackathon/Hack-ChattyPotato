package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.RewriteResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.service.TokenCounter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 사용자 쿼리를 RAG에 최적화된 형태로 재작성하는 서비스
 *
 * <p>목적:
 * - 모호한 쿼리를 명확하게 변환
 * - 검색에 유리한 키워드 추출
 * - 불필요한 표현 제거
 *
 * <p>토큰 제한:
 * - 재작성 쿼리: ≤100 토큰 (전체 예산의 5%)
 * - BERT/BART 점수 비교의 기준, 짧을수록 라우팅 정확도 향상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RewriteChainService {

    private final ChatClient chatClient;

    // 🟦 Rewrite Query: ≤100 tokens (5%)
    private static final int MAX_REWRITE_TOKENS = 100;

    /**
     * 사용자 쿼리를 재작성
     *
     * @param userQuery 원본 사용자 쿼리
     * @return 재작성된 쿼리 정보
     */
    public RewriteResultDto rewrite(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) {
            log.warn("Empty query received, returning as-is");
            return createResult(userQuery, userQuery);
        }

        try {
            log.error("Rewriting query: '{}'", userQuery);

            String rewrittenQuery = rewriteWithLLM(userQuery);

            RewriteResultDto result = createResult(userQuery, rewrittenQuery);

            // 토큰 제한 경고
            if (result.rewrittenTokens() > MAX_REWRITE_TOKENS) {
                log.error("Rewritten query exceeds token limit: {} > {} tokens",
                        result.rewrittenTokens(), MAX_REWRITE_TOKENS);
            }

            log.error("Query rewritten: {} -> {} (tokens: {} -> {})",
                    truncate(userQuery, 50),
                    truncate(rewrittenQuery, 50),
                    result.originalTokens(),
                    result.rewrittenTokens());

            return result;

        } catch (Exception e) {
            log.error("Query rewrite failed: {}. Using original query.", e.getMessage());
            return createResult(userQuery, userQuery);
        }
    }

    /**
     * LLM을 사용해 쿼리 재작성
     */
    private String rewriteWithLLM(String userQuery) {
        String prompt = """
                You are a query optimization assistant for a RAG (Retrieval-Augmented Generation) system.
                
                Your task: Rewrite the user's query to be more effective for document retrieval.
                CRITICAL: Keep the rewritten query under %d tokens. Be extremely concise.
                
                Guidelines:
                - Make the query clear and specific
                - Extract ONLY key search terms and entities
                - Remove ALL conversational filler words
                - Keep important context only
                - Prioritize brevity over completeness
                - Output ONLY the rewritten query, no explanations
                
                User query: %s
                
                Rewritten query (max %d tokens):""".formatted(
                MAX_REWRITE_TOKENS, userQuery, MAX_REWRITE_TOKENS);

        String rewritten = chatClient.prompt(prompt)
                .call()
                .content()
                .trim();

        // 빈 응답 방어
        if (rewritten.isBlank()) {
            log.warn("LLM returned empty rewrite, using original");
            return userQuery;
        }

        return rewritten;
    }

    /**
     * RewriteResultDto 생성 헬퍼
     */
    private RewriteResultDto createResult(String original, String rewritten) {
        int originalTokens = TokenCounter.count(original != null ? original : "");
        int rewrittenTokens = TokenCounter.count(rewritten != null ? rewritten : "");

        return RewriteResultDto.builder()
                .originalQuery(original)
                .rewrittenQuery(rewritten)
                .originalTokens(originalTokens)
                .rewrittenTokens(rewrittenTokens)
                .build();
    }

    /**
     * 로깅용 문자열 자르기
     */
    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}