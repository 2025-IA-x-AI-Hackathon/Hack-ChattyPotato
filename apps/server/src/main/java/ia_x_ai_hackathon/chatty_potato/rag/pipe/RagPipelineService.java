package ia_x_ai_hackathon.chatty_potato.rag.pipe;

import ia_x_ai_hackathon.chatty_potato.common.util.FuturePoller;
import ia_x_ai_hackathon.chatty_potato.rag.dto.*;
import ia_x_ai_hackathon.chatty_potato.rag.exception.PromptBuildFailedException;
import ia_x_ai_hackathon.chatty_potato.rag.exception.PromptTimeoutException;
import ia_x_ai_hackathon.chatty_potato.rag.exception.TaskNotFoundException;
import ia_x_ai_hackathon.chatty_potato.rag.exception.TimeoutException;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.AugmentedChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.GeneratorChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RetrieverChainService;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RewriteChainService;
import ia_x_ai_hackathon.chatty_potato.rag.repository.DocumentRepository;
import ia_x_ai_hackathon.chatty_potato.rag.service.EmbeddingService;
import ia_x_ai_hackathon.chatty_potato.rag.service.VectorStoreService;
import ia_x_ai_hackathon.chatty_potato.rag.store.InMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

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
	private final InMemoryStore inMemoryStore;
	private final VectorStoreService vectorStoreService;
	private final EmbeddingService embeddingService;

	/**
	 * 단일 쿼리에 대한 RAG 전체 실행
	 */
//	public RagResultDto run(String sessionId, String originalQuery) {
//		long start = System.currentTimeMillis();
//		log.info("🚀 RAG Pipeline start for session={}, query='{}'", sessionId, originalQuery);
//
//		try {
////			 1️⃣ Query Rewrite
//            var rewriteResult = rewriteService.rewrite(originalQuery);
//            String rewritten = rewriteResult.rewrittenQuery();
//            log.debug("✏️ Rewritten query: {}", rewritten);
//
//			/// 1
//
//            // 2️⃣ Retrieval
//            List<RetrievedDocumentDto> retrievedDocs = retrieverService.retrieve(rewritten);
//            log.debug("📚 Retrieved {} documents", retrievedDocs.size());
//
//            // 3️⃣ Augmentation
//            AugmentedContextDto augmented = augmentedService.assemble(retrievedDocs);
//            log.debug("🧩 Context assembled ({} chars)", augmented.contextText().length());
//
//            // 4️⃣ Prompt assembly + Generation
//            PromptAssemblyDto prompt = generatorService.generatePrompt(
//                    originalQuery, rewritten, augmented
//            );
//
//			/// 2
//
//			String answer = generatorService.generateAnswer(prompt);
//
//			// 5️⃣ 결과 구성
//			RagResultDto result = new RagResultDto(
//					sessionId,
//					originalQuery,
//					rewritten,
//					answer,
//					prompt,
//					augmented.citations(),
//					Instant.now()
//			);
//
//			long took = System.currentTimeMillis() - start;
//			log.info("✅ RAG completed in {} ms ({} docs, {} chars output)",
//					took, retrievedDocs.size(), answer.length());
//
//			return result;
//
//		} catch (Exception e) {
//			log.error("❌ Pipeline failed: {}", e.getMessage(), e);
//			return RagResultDto.failed(sessionId, originalQuery, e.getMessage());
//		}
//	}
	public RewriteResDto rewriteQuery(String userId, String originalQuery) {
		String taskId = UUID.randomUUID().toString();

		RewriteResultDto rewriteResult = rewriteService.rewrite(originalQuery);
		String rewritten = rewriteResult.rewrittenQuery();
		log.debug("✏️ Rewritten query: {}", rewriteResult.rewrittenQuery());

		inMemoryStore.init(userId, taskId, originalQuery, rewritten);
		asyncBuildPrompt(userId, taskId, originalQuery, rewritten);

		return new RewriteResDto(taskId, rewritten);
	}

	@Async("ragExecutor")
	protected void asyncBuildPrompt(String userId, String taskId, String original, String rewritten) {
		if (!inMemoryStore.markBuildStarted(userId, taskId)) {
			log.debug("⏭️ prompt build already started (userId={}, taskId={})", userId, taskId);
			return;
		}

		try {
			List<RetrievedDocumentDto> retrievedDocs = retrieverService.retrieve(rewritten);
			log.debug("📚 Retrieved {} documents", retrievedDocs.size());

			// 3️⃣ Augmentation
			AugmentedContextDto augmented = augmentedService.assemble(retrievedDocs);
			log.debug("🧩 Context assembled ({} chars)", augmented.contextText().length());

			// 4️⃣ Prompt assembly + Generation
			PromptAssemblyDto prompt = generatorService.generatePrompt(
					original, rewritten, augmented
			);

			inMemoryStore.completePrompt(userId, taskId, prompt, augmented);
			log.info("✅ prompt READY (userId={}, taskId={})", userId, taskId);
		} catch (Exception e) {
			log.error("❌ prompt build failed (userId={}, taskId={}): {}", userId, taskId, e.getMessage(), e);
			inMemoryStore.failPrompt(userId, taskId, e.getMessage());
		}
	}

	public PromptAssemblyDto awaitPrompt(String userId, String taskId, long waitMillis, long stepMillis) {
		var slot = inMemoryStore.get(userId, taskId)
				.orElseThrow(() -> new TaskNotFoundException(userId, taskId));

		if (slot.getStatus() == InMemoryStore.Status.ERROR) {
			throw new PromptBuildFailedException(userId, taskId, slot.getError());
		}

		try {
			Object prompt = FuturePoller.awaitWithDeadline(
					slot.getPromptFuture(),
					waitMillis,
					stepMillis,
					() -> slot.getStatus() == InMemoryStore.Status.ERROR
			);

			if (prompt == null) {
				throw new PromptBuildFailedException(userId, taskId, "Prompt resolved to null");
			}
			return (PromptAssemblyDto) prompt;

		} catch (TimeoutException te) {
			throw new PromptTimeoutException(userId, taskId, waitMillis);

		} catch (ExecutionException ee) {
			throw new PromptBuildFailedException(
					userId,
					taskId,
					ee.getCause() != null ? ee.getCause().getMessage() : ee.getMessage()
			);

		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new PromptBuildFailedException(userId, taskId, "Interrupted while waiting for prompt");
		}
	}

	public RagResultDto produce(String userId, String taskId, boolean isLow, long waitMillis, long stepMillis) {
		// 1) 데드라인-폴링으로 프롬프트 확보
		PromptAssemblyDto prompt = awaitPrompt(userId, taskId, waitMillis, stepMillis);

		// 2) (옵션) 원문/리라이트는 슬롯에서 회수
		var slot = inMemoryStore.get(userId, taskId)
				.orElseThrow(() -> new TaskNotFoundException(userId, taskId));



		if (isLow) {
			return new RagResultDto(
					taskId,                // sessionId로 taskId 사용
					slot.getOriginal(),    // originalQuery
					slot.getRewritten(),   // rewrittenQuery
					null,                // answer
					prompt,                // prompt dto
					slot.getAugmentedContext().citations(),    // citations
					Instant.now()
			);
		}

			// 3) 하이 LLM 동기 호출
			String answer = generatorService.generateAnswer(prompt);

		vectorStoreService.save(
				answer,
				Map.of(
						"originalQuery", slot.getOriginal(),
						"rewrittenQuery", slot.getRewritten(),
						"taskId", taskId,
						"userId", userId,
						"createdAt", Instant.now().toString()
				)
		);

		// 4) DTO 조립
		return new RagResultDto(
				taskId,                // sessionId로 taskId 사용
				slot.getOriginal(),    // originalQuery
				slot.getRewritten(),   // rewrittenQuery
				answer,                // answer
				prompt,                // prompt dto
				slot.getAugmentedContext().citations(),    // citations
				Instant.now()
		);
	}

}

