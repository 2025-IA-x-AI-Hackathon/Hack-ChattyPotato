package ia_x_ai_hackathon.chatty_potato.rag.service;

import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * RetrievedDocument가 너무 길 경우,
 * 핵심 내용만 보존한 condensed snippet으로 변환하는 역할.
 *
 * <p>요약 기준:
 * - 2000자(~1000 토큰) 이상인 문서만 요약
 * - 요약 후 목표: 512 토큰 이하
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizationService {

    private final ChatClient chatClient;

    private static final int SUMMARIZE_THRESHOLD_CHARS = 1250; // 2000자 이상이면 요약
    private static final int MAX_SNIPPET_TOKENS = 1100;          // 요약 후 최대 토큰

    /**
     * 문서가 길면 요약, 짧으면 그대로 반환
     *
     * @param doc 원본 문서
     * @return 요약된 문서 또는 원본 문서
     */
    public RetrievedDocumentDto summarizeIfNeeded(RetrievedDocumentDto doc) {
        if (doc.snippet() == null || doc.snippet().length() < SUMMARIZE_THRESHOLD_CHARS) {
            log.debug("Doc[{}] is short ({} chars), skipping summarization",
                    doc.id(), doc.snippet() != null ? doc.snippet().length() : 0);
            return doc;
        }

        try {
            log.debug("Summarizing doc[{}]: {} chars -> target {} tokens",
                    doc.id(), doc.snippet().length(), MAX_SNIPPET_TOKENS);

            String summary = summarize(doc.snippet());

            int originalTokens = TokenCounter.count(doc.snippet());
            int summaryTokens = TokenCounter.count(summary);

            log.info("Summarized doc[{}]: {} -> {} tokens ({} chars -> {} chars)",
                    doc.id(), originalTokens, summaryTokens,
                    doc.snippet().length(), summary.length());

            return new RetrievedDocumentDto(
                    doc.id(),
                    doc.title(),
                    summary,
                    doc.url(),  // ✅ url 필드 추가
                    doc.score()
            );

        } catch (Exception e) {
            log.warn("Summarization failed for doc[{}]: {}. Using original snippet.",
                    doc.id(), e.getMessage());
            return doc; // ✅ 실패 시 원본 반환 (안정성)
        }
    }

    /**
     * LLM을 사용해 텍스트 요약
     */
    private String summarize(String text) {
        String prompt = """
                You are a summarization assistant for a retrieval system.
                Summarize the following text into a concise but information-rich version (max %d tokens).
                Keep named entities, numeric data, and factual content.
                Respond with ONLY the summary, no additional text or explanations.
                
                Text:
                %s
                """.formatted(MAX_SNIPPET_TOKENS, text);

        return chatClient.prompt(prompt)
                .call()
                .content()
                .trim();
    }

    /**
     * 여러 문서를 한꺼번에 요약 처리
     *
     * @param docs 원본 문서 리스트
     * @return 요약된 문서 리스트 (일부는 원본 그대로)
     */
    public List<RetrievedDocumentDto> summarizeBatch(List<RetrievedDocumentDto> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        log.info("Summarization batch: processing {} docs", docs.size());

        long startTime = System.currentTimeMillis();

        List<RetrievedDocumentDto> result = docs.stream()
                .map(this::summarizeIfNeeded)
                .toList();

        long elapsedMs = System.currentTimeMillis() - startTime;
        long summarizedCount = result.stream()
                .filter(doc -> !doc.snippet().equals(
                        docs.stream()
                                .filter(orig -> orig.id().equals(doc.id()))
                                .findFirst()
                                .map(RetrievedDocumentDto::snippet)
                                .orElse("")
                ))
                .count();

        log.info("Summarization batch completed: {} out of {} docs summarized in {}ms",
                summarizedCount, docs.size(), elapsedMs);

        return result;
    }
}