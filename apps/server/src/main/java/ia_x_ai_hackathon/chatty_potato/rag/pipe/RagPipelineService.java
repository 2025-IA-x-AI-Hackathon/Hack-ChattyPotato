package ia_x_ai_hackathon.chatty_potato.rag.pipe;

import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.PromptAssemblyDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RagResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.AugmentedChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.GeneratorChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RetrieverChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RewriteChainService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 🔗 전체 RAG 파이프라인 오케스트레이터
 *
 * Rewrite → Retrieval → Augmentation → Generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagPipelineService {

    private final RewriteChainService rewriteService;      // 사용자 질의 재작성
    private final RetrieverChainService retrieverService;        // ES 기반 문서 검색
    private final AugmentedChainService augmentedService;   // context 조립
    private final GeneratorChainService generatorService;        // LLM 호출

    /**
     * 단일 쿼리에 대한 RAG 전체 실행
     */
    public RagResultDto run(String sessionId, String originalQuery) {
        long start = System.currentTimeMillis();
        log.info("🚀 RAG Pipeline start for session={}, query='{}'", sessionId, originalQuery);

        try {
            // 1️⃣ Query Rewrite
            var rewriteResult = rewriteService.rewrite(originalQuery);
            String rewritten = rewriteResult.rewrittenQuery();
            log.debug("✏️ Rewritten query: {}", rewritten);

            // 2️⃣ Retrieval
            List<RetrievedDocumentDto> retrievedDocs = retrieverService.retrieve(rewritten);
            log.debug("📚 Retrieved {} documents", retrievedDocs.size());

            // 3️⃣ Augmentation
            AugmentedContextDto augmented = augmentedService.assemble(retrievedDocs);
            log.debug("🧩 Context assembled ({} chars)", augmented.contextText().length());

            // 4️⃣ Prompt assembly + Generation
            PromptAssemblyDto prompt = generatorService.generatePrompt(
                    originalQuery, rewritten, augmented
            );
            String answer = generatorService.generateAnswer(prompt);

            // 5️⃣ 결과 구성
            RagResultDto result = new RagResultDto(
                    sessionId,
                    originalQuery,
                    rewritten,
                    answer,
                    prompt,
                    augmented.citations(),
                    Instant.now()
            );

            long took = System.currentTimeMillis() - start;
            log.info("✅ RAG completed in {} ms ({} docs, {} chars output)",
                    took, retrievedDocs.size(), answer.length());

            return result;

        } catch (Exception e) {
            log.error("❌ Pipeline failed: {}", e.getMessage(), e);
            return RagResultDto.failed(sessionId, originalQuery, e.getMessage());
        }
    }
}
