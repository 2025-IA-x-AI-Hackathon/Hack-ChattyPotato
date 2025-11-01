package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.EmbeddingResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.entity.DocumentEntity;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.RetrieverChainService;
import ia_x_ai_hackathon.chatty_potato.rag.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.stream.Stream;
import org.springframework.data.elasticsearch.core.query.Query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RetrieverChainServiceTest {

    private EmbeddingService embeddingService;
    private ElasticsearchOperations esOps;
    private RetrieverChainService service;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        esOps = mock(ElasticsearchOperations.class, Answers.RETURNS_DEEP_STUBS);
        service = new RetrieverChainService(embeddingService, esOps);
    }

    @Test
    @DisplayName("빈 쿼리 → 빈 결과")
    void empty_query_returns_empty() {
        var out1 = service.retrieve(null);
        var out2 = service.retrieve("  ", 3);

        assertThat(out1).isEmpty();
        assertThat(out2).isEmpty();
        verifyNoInteractions(embeddingService, esOps);
    }

    @Test
    @DisplayName("정상 검색 → ES hits를 DTO로 변환, MIN_RELEVANCE_SCORE 이상만 통과")
    void retrieve_filters_and_maps_hits() {
        // given
        when(embeddingService.embed("hello")).thenReturn(
                new EmbeddingResultDto("id", new float[]{1f, 2f}, 2)
        );

        // ES SearchHits mock
        SearchHits<DocumentEntity> hits = mock(SearchHits.class);
        when(esOps.search((Query) any(), eq(DocumentEntity.class))).thenReturn(hits);

        // hit1: score 0.8 (통과)
        var h1 = mockHit(doc("1", "T1", "C1", "u1"), 0.8f);
        // hit2: score 0.3 (필터 아웃)
        var h2 = mockHit(doc("2", "T2", "C2", "u2"), 0.3f);
        // hit3: score 1.2 (정규화 전에 넘어오지만, 코드에서 clamp하진 않고 그대로 normalizeScore→clamp 0~1)
        var h3 = mockHit(doc("3", "T3", "C3", "u3"), 1.2f);

        when(hits.stream()).thenReturn(Stream.of(h1, h2, h3));

        // when
        var out = service.retrieve("hello", 5);

        // then
        assertThat(out).hasSize(2); // 0.8, 1.2 -> 2개 통과
        // 정렬은 보장 안하지만, 각 id/title/snippet/url 매핑 확인
        assertThat(out).anySatisfy(d -> {
            assertThat(d.id()).isEqualTo("1");
            assertThat(d.title()).isEqualTo("T1");
            assertThat(d.snippet()).isEqualTo("C1");
            assertThat(d.url()).isEqualTo("u1");
            assertThat(d.score()).isBetween(0.0, 1.0); // normalizeScore 적용
        });
        assertThat(out).anySatisfy(d -> assertThat(d.id()).isEqualTo("3"));

        verify(embeddingService, times(1)).embed("hello");
        verify(esOps, times(1)).search((Query) any(), eq(DocumentEntity.class));
    }

    @Test
    @DisplayName("topK <= 0 → 기본값 사용")
    void invalid_topk_uses_default() {
        when(embeddingService.embed(anyString())).thenReturn(
                new EmbeddingResultDto("id", new float[]{0.1f}, 1)
        );
        SearchHits<DocumentEntity> hits = mock(SearchHits.class);
        when(esOps.search((Query) any(), eq(DocumentEntity.class))).thenReturn(hits);
        when(hits.stream()).thenReturn(Stream.empty());

        var out = service.retrieve("q", 0);
        assertThat(out).isEmpty();

        verify(embeddingService).embed("q");
        verify(esOps).search((Query) any(), eq(DocumentEntity.class));
    }

    @Test
    @DisplayName("ES 예외 → 빈 리스트 반환 (안정성)")
    void es_error_returns_empty() {
        when(embeddingService.embed(anyString())).thenReturn(
                new EmbeddingResultDto("id", new float[]{0.1f}, 1)
        );
        when(esOps.search((Query) any(), eq(DocumentEntity.class))).thenThrow(new RuntimeException("ES down"));

        var out = service.retrieve("q", 3);
        assertThat(out).isEmpty();
    }

    // helpers
    private SearchHit<DocumentEntity> mockHit(DocumentEntity doc, float score) {
        @SuppressWarnings("unchecked")
        SearchHit<DocumentEntity> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hit.getScore()).thenReturn((float) score);
        return hit;
    }

    private DocumentEntity doc(String id, String title, String content, String url) {
        return DocumentEntity.builder()
                .id(id)
                .title(title)
                .content(content)
                .url(url)
                .embedding(null)
                .build();
    }
}
