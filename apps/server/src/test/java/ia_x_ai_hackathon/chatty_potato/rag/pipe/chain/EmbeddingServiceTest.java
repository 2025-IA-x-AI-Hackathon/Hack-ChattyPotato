//package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;
//
//import ia_x_ai_hackathon.chatty_potato.rag.service.EmbeddingService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.mockito.Answers;
//
//import java.util.List;
//
//import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
//import org.springframework.ai.embedding.EmbeddingRequest;
//import org.springframework.ai.embedding.EmbeddingResponse;
//import org.springframework.ai.embedding.Embedding;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//class EmbeddingServiceTest {
//
//    private BedrockTitanEmbeddingModel model;
//    private EmbeddingService service;
//
//    @BeforeEach
//    void setUp() {
//        // deep stubs: response.getResults().get(0).getOutput() 같은 체인을 쉽게 모킹
//        model = mock(BedrockTitanEmbeddingModel.class, Answers.RETURNS_DEEP_STUBS);
//        service = new EmbeddingService(model);
//    }
//
//    @Test
//    @DisplayName("빈 문자열 임베딩 → zero vector(1024) 반환")
//    void embed_blank_returns_zero_vector() {
//        var result = service.embed("   ");
//
//        assertThat(result.id()).isEqualTo("zero");
//        assertThat(result.vector()).hasSize(1024); // EMBEDDING_DIMENSIONS
//        assertThat(result.dims()).isEqualTo(1024);
//        verifyNoInteractions(model);
//    }
//
//    @Test
//    @DisplayName("정상 임베딩 → 첫 결과를 반환")
//    void embed_normal() {
//        // given
//        EmbeddingResponse response = mock(EmbeddingResponse.class);
//        Embedding embedding = mock(Embedding.class);
//        when(model.call(any(EmbeddingRequest.class))).thenReturn(response);
//        when(response.getResults()).thenReturn(List.of(embedding));
//        when(embedding.getOutput()).thenReturn(new float[]{1f, 2f, 3f});
//        when(embedding.getIndex()).thenReturn(0);
//
//        // when
//        var out = service.embed("hello world");
//
//        // then
//        assertThat(out.vector()).containsExactly(1f, 2f, 3f);
//        assertThat(out.dims()).isEqualTo(3);
//        assertThat(out.id()).startsWith("emb-");
//        verify(model, times(1)).call(any(EmbeddingRequest.class));
//    }
//
//    @Test
//    @DisplayName("배치 임베딩 → 각 index/벡터 반환")
//    void embed_batch_normal() {
//        // given
//        var texts = List.of("A", "B");
//
//        EmbeddingResponse response = mock(EmbeddingResponse.class);
//        Embedding e0 = mock(Embedding.class);
//        Embedding e1 = mock(Embedding.class);
//
//        when(model.call(any(EmbeddingRequest.class))).thenReturn(response);
//        when(response.getResults()).thenReturn(List.of(e0, e1));
//        when(e0.getOutput()).thenReturn(new float[]{10f});
//        when(e0.getIndex()).thenReturn(0);
//        when(e1.getOutput()).thenReturn(new float[]{20f, 21f});
//        when(e1.getIndex()).thenReturn(1);
//
//        // when
//        var out = service.embedBatch(texts);
//
//        // then
//        assertThat(out).hasSize(2);
//        assertThat(out.get(0).vector()).containsExactly(10f);
//        assertThat(out.get(1).vector()).containsExactly(20f, 21f);
//        verify(model, times(1)).call(any(EmbeddingRequest.class));
//    }
//
//    @Test
//    @DisplayName("배치 임베딩 실패 시 → 개별 임베딩 fallback")
//    void embed_batch_fallback_to_individual() {
//        var texts = List.of("X", "Y");
//
//        // 1) 배치 호출은 예외
//        when(model.call(any(EmbeddingRequest.class))).thenThrow(new RuntimeException("boom"))
//                // 2) 이후 개별 임베딩 X
//                .thenReturn(mockSingleResponse(new float[]{7f}, 0))
//                // 3) 이후 개별 임베딩 Y
//                .thenReturn(mockSingleResponse(new float[]{9f, 9f}, 0));
//
//        var out = service.embedBatch(texts);
//
//        assertThat(out).hasSize(2);
//        assertThat(out.get(0).vector()).containsExactly(7f);
//        assertThat(out.get(1).vector()).containsExactly(9f, 9f);
//        // 총 3번 call: (배치 1) + (개별 2)
//        verify(model, times(3)).call(any(EmbeddingRequest.class));
//    }
//
//    // helper: 단일 결과용 EmbeddingResponse mock
//    private EmbeddingResponse mockSingleResponse(float[] vec, int index) {
//        EmbeddingResponse r = mock(EmbeddingResponse.class);
//        Embedding e = mock(Embedding.class);
//        when(r.getResults()).thenReturn(List.of(e));
//        when(e.getOutput()).thenReturn(vec);
//        when(e.getIndex()).thenReturn(index);
//        return r;
//    }
//}
//
