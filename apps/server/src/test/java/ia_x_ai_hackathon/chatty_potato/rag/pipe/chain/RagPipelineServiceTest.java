package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.*;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.RagPipelineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.List;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RagPipelineService 오케스트레이션 테스트")
class RagPipelineServiceTest {

    @Mock private RewriteChainService rewriteService;
    @Mock private RetrieverChainService retrieverService;
    @Mock private AugmentedChainService augmentedService;
    @Mock private GeneratorChainService generatorService;

    @InjectMocks private RagPipelineService pipeline;

    @Test
    @DisplayName("✅ 정상 플로우: rewrite→retrieve→augment→generate")
    void run_success() {
        // given
        String sessionId = "sess-1";
        String original  = "원 질문은 무엇인가?";
        String rewritten = "rewritten-query";

        var rewriteDto = RewriteResultDto.builder()
                .originalQuery(original)
                .rewrittenQuery(rewritten)
                .originalTokens(10)
                .rewrittenTokens(8)
                .build();

        List<RetrievedDocumentDto> docs = List.of(
                RetrievedDocumentDto.builder()
                        .id("d1").title("T1").snippet("S1").url("u1").score(0.9)
                        .build()
        );

        var augmented = AugmentedContextDto.builder()
                .contextText("CTX")
                .citations(List.of())
                .build();

        var prompt = new PromptAssemblyDto("SYS","USER","RW","CTX","FINAL_PROMPT");

        when(rewriteService.rewrite(original)).thenReturn(rewriteDto);
        when(retrieverService.retrieve(rewritten)).thenReturn(docs);
        when(augmentedService.assemble(docs)).thenReturn(augmented);
        when(generatorService.generatePrompt(original, rewritten, augmented)).thenReturn(prompt);
        when(generatorService.generateAnswer(prompt)).thenReturn("FINAL_ANSWER");

        // when
        RagResultDto result = pipeline.run(sessionId, original);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.originalQuery()).isEqualTo(original);
        assertThat(result.rewrittenQuery()).isEqualTo(rewritten);
        assertThat(result.answer()).isEqualTo("FINAL_ANSWER");
        assertThat(result.prompt()).isNotNull();
        assertThat(result.citations()).isNotNull();
        assertThat(result.createdAt()).isBeforeOrEqualTo(Instant.now());

        verify(rewriteService).rewrite(original);
        verify(retrieverService).retrieve(rewritten);
        verify(augmentedService).assemble(docs);
        verify(generatorService).generatePrompt(original, rewritten, augmented);
        verify(generatorService).generateAnswer(prompt);
        verifyNoMoreInteractions(rewriteService, retrieverService, augmentedService, generatorService);
    }

    @Test
    @DisplayName("🪄 검색 결과가 비어도 끝까지 생성")
    void run_empty_retrieval_still_generates() {
        // given
        String sessionId = "sess-empty";
        String original  = "무슨 질문";
        String rewritten = "rw";

        var rewriteDto = RewriteResultDto.builder()
                .originalQuery(original)
                .rewrittenQuery(rewritten)
                .originalTokens(6)
                .rewrittenTokens(2)
                .build();

        var augmented = AugmentedContextDto.builder()
                .contextText("") // empty context
                .citations(List.of())
                .build();

        var prompt = new PromptAssemblyDto("SYS","USER","RW","","FINAL_PROMPT");

        when(rewriteService.rewrite(original)).thenReturn(rewriteDto);
        when(retrieverService.retrieve(rewritten)).thenReturn(List.of());
        when(augmentedService.assemble(List.of())).thenReturn(augmented);
        when(generatorService.generatePrompt(original, rewritten, augmented)).thenReturn(prompt);
        when(generatorService.generateAnswer(prompt)).thenReturn("ANS_WITHOUT_CONTEXT");

        // when
        RagResultDto result = pipeline.run(sessionId, original);

        // then
        assertThat(result.answer()).isEqualTo("ANS_WITHOUT_CONTEXT");
        verify(rewriteService).rewrite(original);
        verify(retrieverService).retrieve(rewritten);
        verify(augmentedService).assemble(List.of());
        verify(generatorService).generatePrompt(original, rewritten, augmented);
        verify(generatorService).generateAnswer(prompt);
        verifyNoMoreInteractions(rewriteService, retrieverService, augmentedService, generatorService);
    }

    @Test
    @DisplayName("⚠️ 중간 예외 발생 시 실패 결과 반환")
    void run_failure_returns_failed() {
        // given
        String sessionId = "sess-err";
        String original  = "질문";
        String rewritten = "rw";

        var rewriteDto = RewriteResultDto.builder()
                .originalQuery(original)
                .rewrittenQuery(rewritten)
                .originalTokens(3)
                .rewrittenTokens(2)
                .build();

        when(rewriteService.rewrite(original)).thenReturn(rewriteDto);
        when(retrieverService.retrieve(rewritten)).thenThrow(new RuntimeException("ES down"));

        // when
        RagResultDto result = pipeline.run(sessionId, original);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.originalQuery()).isEqualTo(original);
        assertThat(result.answer()).contains("Internal Error");

        verify(rewriteService).rewrite(original);
        verify(retrieverService).retrieve(rewritten);
        verifyNoInteractions(augmentedService, generatorService);
        verifyNoMoreInteractions(rewriteService, retrieverService);
    }
}
