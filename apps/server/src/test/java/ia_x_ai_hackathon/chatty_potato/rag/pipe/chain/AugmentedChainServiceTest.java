package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.chain.AugmentedChainService;
import ia_x_ai_hackathon.chatty_potato.rag.service.SummarizationService;
import ia_x_ai_hackathon.chatty_potato.rag.service.TokenAllocationStrategy;
import ia_x_ai_hackathon.chatty_potato.rag.service.TokenAllocationStrategy.TokenAllocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AugmentedChainService - 단일 정책/assemble(List) 최소 동작 테스트")
class AugmentedChainServiceTest {

    @Mock
    private SummarizationService summarizationService;

    @Mock
    private TokenAllocationStrategy tokenAllocationStrategy;

    @InjectMocks
    private AugmentedChainService augmentedChainService;

    @BeforeEach
    void setUp() {
        lenient().when(tokenAllocationStrategy.allocate())
                .thenReturn(new TokenAllocation(200,100,200,500,1000,512));

        lenient().when(summarizationService.summarizeBatch(any()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("✅ 최소 동작: 결과 null 아님")
    void service_returns_non_null_result() {
        List<RetrievedDocumentDto> docs = List.of(createDoc("1", "Test content"));
        AugmentedContextDto result = augmentedChainService.assemble(docs);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("✅ 빈 리스트 처리")
    void handles_empty_list() {
        AugmentedContextDto result = augmentedChainService.assemble(List.of());
        assertThat(result).isNotNull();
        assertThat(result.contextText()).isEmpty();
        assertThat(result.citations()).isEmpty();
    }

    @Test
    @DisplayName("✅ 단일 문서 처리")
    void handles_single_document() {
        List<RetrievedDocumentDto> docs = List.of(createDoc("1", "Single doc"));
        AugmentedContextDto result = augmentedChainService.assemble(docs);
        assertThat(result).isNotNull();
        assertThat(result.citations()).isNotEmpty();
        assertThat(result.contextText()).contains("Source[1]:");
    }

    @Test
    @DisplayName("✅ 여러 문서 처리")
    void handles_multiple_documents() {
        List<RetrievedDocumentDto> docs = List.of(
                createDoc("1", "First"),
                createDoc("2", "Second"),
                createDoc("3", "Third")
        );
        AugmentedContextDto result = augmentedChainService.assemble(docs);
        assertThat(result).isNotNull();
        assertThat(result.citations()).isNotEmpty();
        assertThat(result.contextText()).contains("Source[1]:").contains("Source[2]:");
    }

    @Test
    @DisplayName("✅ SummarizationService 호출")
    void calls_summarization_service() {
        List<RetrievedDocumentDto> docs = List.of(createDoc("1", "Doc"));
        augmentedChainService.assemble(docs);
        verify(summarizationService, atLeastOnce()).summarizeBatch(any());
    }

    @Test
    @DisplayName("✅ TokenAllocationStrategy 호출")
    void calls_token_allocation_strategy() {
        List<RetrievedDocumentDto> docs = List.of(createDoc("1", "Doc"));
        augmentedChainService.assemble(docs);
        verify(tokenAllocationStrategy, atLeastOnce()).allocate();
    }

    @Test
    @DisplayName("🔍 상세 디버깅 출력")
    void debug_actual_output() {
        List<RetrievedDocumentDto> docs = List.of(
                createDoc("1", "First document with some content"),
                createDoc("2", "Second document with more content"),
                createDoc("3", "Third document")
        );
        AugmentedContextDto result = augmentedChainService.assemble(docs);

        System.out.println("\n🔍 === 실제 반환값 상세 정보 ===");
        System.out.println("📄 ContextText:\n" + result.contextText());
        System.out.println("\n📚 Citations (" + result.citations().size() + "개):");
        result.citations().forEach(c -> System.out.println("  - " + c));
        System.out.println("================================\n");

        assertThat(result).isNotNull();
    }

    private RetrievedDocumentDto createDoc(String id, String snippet) {
        return RetrievedDocumentDto.builder()
                .id(id)
                .title("Title " + id)
                .snippet(snippet)
                .url("http://example.com/" + id)
                .score(1.0 - Integer.parseInt(id) * 0.1)
                .build();
    }
}
