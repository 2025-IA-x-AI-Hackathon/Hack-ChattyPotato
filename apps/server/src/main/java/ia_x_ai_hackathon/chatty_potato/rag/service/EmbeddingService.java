package ia_x_ai_hackathon.chatty_potato.rag.service;

import ia_x_ai_hackathon.chatty_potato.rag.dto.EmbeddingResultDto;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

/**
 * AWS Bedrock Titan Embeddings를 사용한 임베딩 서비스
 *
 * <p>사용 모델: amazon.titan-embed-text-v1 (1536 dimensions)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final BedrockTitanEmbeddingModel embeddingModel;

    private static final int EMBEDDING_DIMENSIONS = 1024; // Titan v1 기본 차원

    /**
     * 텍스트를 벡터로 변환
     *
     * @param text 임베딩할 텍스트
     * @return 임베딩 결과 (벡터)
     */
    public EmbeddingResultDto embed(String text) {
        if (text == null || text.isBlank()) {
            log.warn("Empty text received for embedding, returning zero vector");
            return createZeroVector();
        }

        try {
            log.debug("Embedding text: '{}'", truncate(text, 50));

            // Spring AI의 EmbeddingRequest 사용
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null)
            );

            // 첫 번째 결과 추출
            float[] vector = response.getResults().get(0).getOutput();

            String embeddingId = generateEmbeddingId(text);

            log.debug("Embedding created: id={}, dimensions={}", embeddingId, vector.length);

            return new EmbeddingResultDto(embeddingId, vector, vector.length);

        } catch (Exception e) {
            log.error("Embedding failed for text: '{}'. Error: {}",
                    truncate(text, 50), e.getMessage(), e);

            // 실패 시 제로 벡터 반환 (안정성)
            return createZeroVector();
        }
    }

    /**
     * 배치 임베딩: 여러 텍스트를 한 번에 변환
     *
     * @param texts 임베딩할 텍스트 리스트
     * @return 임베딩 결과 리스트
     */
    public List<EmbeddingResultDto> embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("Empty texts received for batch embedding");
            return List.of();
        }

        log.info("Batch embedding: {} texts", texts.size());

        try {
            // Spring AI는 배치 요청 지원
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(texts, null)
            );

            // 각 결과를 EmbeddingResultDto로 변환
            List<EmbeddingResultDto> results = response.getResults().stream()
                    .map(result -> {
                        float[] vector = result.getOutput();
                        String id = generateEmbeddingId(texts.get(result.getIndex()));
                        return new EmbeddingResultDto(id, vector, vector.length);
                    })
                    .collect(Collectors.toList());

            log.info("Batch embedding completed: {} results", results.size());

            return results;

        } catch (Exception e) {
            log.error("Batch embedding failed: {}", e.getMessage(), e);

            // 실패 시 각 텍스트를 개별적으로 임베딩 (fallback)
            log.warn("Falling back to individual embeddings");
            return texts.stream()
                    .map(this::embed)
                    .collect(Collectors.toList());
        }
    }

    /**
     * 제로 벡터 생성 (에러 핸들링용)
     */
    private EmbeddingResultDto createZeroVector() {
        float[] zeroVector = new float[EMBEDDING_DIMENSIONS];
        return new EmbeddingResultDto("zero", zeroVector, EMBEDDING_DIMENSIONS);
    }

    /**
     * 임베딩 ID 생성 (텍스트 해시 기반)
     */
    private String generateEmbeddingId(String text) {
        if (text == null) {
            return "emb-null";
        }
        // 간단한 해시 기반 ID (실제로는 UUID 등 사용 가능)
        return "emb-" + Math.abs(text.hashCode());
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