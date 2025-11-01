package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.EmbeddingResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import ia_x_ai_hackathon.chatty_potato.rag.entity.DocumentEntity;
import ia_x_ai_hackathon.chatty_potato.rag.service.EmbeddingService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * RAG 파이프라인의 Retrieval 단계를 담당하는 서비스 (Elasticsearch 기반)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetrieverChainService {

    private final EmbeddingService embeddingService;
    private final ElasticsearchOperations elasticsearchOperations;

    private static final String INDEX_NAME = "documents";
    private static final int DEFAULT_TOP_K = 5;
    private static final double MIN_RELEVANCE_SCORE = 0.5;

    public List<RetrievedDocumentDto> retrieve(String query) {
        return retrieve(query, DEFAULT_TOP_K);
    }

    public List<RetrievedDocumentDto> retrieve(String query, int topK) {
        if (query == null || query.isBlank()) {
            log.warn("Empty query received, returning empty results");
            return List.of();
        }

        if (topK <= 0) {
            log.warn("Invalid topK: {}, using default: {}", topK, DEFAULT_TOP_K);
            topK = DEFAULT_TOP_K;
        }

        try {
            log.debug("Retrieving documents for query: '{}', topK: {}", query, topK);

            EmbeddingResultDto queryEmbedding = embeddingService.embed(query);
            log.debug("Query embedded: {} dimensions", queryEmbedding.dims());

            List<RetrievedDocumentDto> results = searchWithKnn(queryEmbedding.vector(), topK);

            List<RetrievedDocumentDto> filteredResults = results.stream()
                    .filter(doc -> doc.score() >= MIN_RELEVANCE_SCORE)
                    .collect(Collectors.toList());

            log.info("Retrieved {} documents (filtered from {}, topK: {})",
                    filteredResults.size(), results.size(), topK);

            if (filteredResults.isEmpty()) {
                log.warn("No documents met minimum relevance score: {}", MIN_RELEVANCE_SCORE);
            }

            return filteredResults;

        } catch (Exception e) {
            log.error("Document retrieval failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    private List<RetrievedDocumentDto> searchWithKnn(float[] queryVector, int topK) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .knn(knn -> knn
                                .field("embedding")
                                .queryVector(toFloatList(queryVector))
                                .k(topK)
                                .numCandidates(topK * 10)
                        )
                )
                .withMaxResults(topK)
                .build();

        // ✅ IndexCoordinates 대신 간단하게 처리
        SearchHits<DocumentEntity> searchHits = elasticsearchOperations.search(
                query,
                DocumentEntity.class
        );

        return searchHits.stream()
                .map(this::toRetrievedDocument)
                .collect(Collectors.toList());
    }

    private RetrievedDocumentDto toRetrievedDocument(SearchHit<DocumentEntity> hit) {
        DocumentEntity doc = hit.getContent();
        double score = hit.getScore();

        return RetrievedDocumentDto.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .snippet(doc.getContent())
                .url(doc.getUrl())
                .score(normalizeScore(score))
                .build();
    }

    private double normalizeScore(double rawScore) {
        return Math.max(0.0, Math.min(1.0, rawScore));
    }

    private List<Float> toFloatList(float[] floats) {
        return IntStream.range(0, floats.length)
                .mapToObj(i -> floats[i])
                .collect(Collectors.toList());
    }

    public List<List<RetrievedDocumentDto>> retrieveBatch(List<String> queries, int topK) {
        if (queries == null || queries.isEmpty()) {
            log.warn("Empty queries received for batch retrieval");
            return List.of();
        }

        log.info("Batch retrieval: {} queries, topK: {}", queries.size(), topK);

        return queries.stream()
                .map(query -> retrieve(query, topK))
                .collect(Collectors.toList());
    }

    public List<RetrievedDocumentDto> retrieveHybrid(String query, int topK) {
        log.debug("Hybrid retrieval (Vector only for now) for query: '{}'", query);
        return retrieve(query, topK);
    }
}