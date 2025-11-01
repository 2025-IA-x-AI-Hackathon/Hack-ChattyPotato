package ia_x_ai_hackathon.chatty_potato.rag.service;

import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private SummarizationService service;

    @BeforeEach
    void setUp() {
        service = new SummarizationService(chatClient);
        // ✅ setUp에서 모킹 제거 - 각 테스트에서 필요할 때만 설정
    }

    @Test
    @DisplayName("긴 문서(2000자 이상)는 LLM을 통해 요약됨")
    void long_document_gets_summarized() {
        // given: 3000자 긴 문서
        String longText = "A".repeat(3000);
        RetrievedDocumentDto longDoc = RetrievedDocumentDto.builder()
                .id("doc-1")
                .title("Long Document")
                .snippet(longText)
                .url("https://example.com/doc-1")
                .score(0.95)
                .build();

        // ✅ 이 테스트에서만 모킹 설정
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("This is a summarized version");

        // when: 요약 실행
        RetrievedDocumentDto result = service.summarizeIfNeeded(longDoc);

        // then: 요약이 적용됨
        assertThat(result.snippet()).isEqualTo("This is a summarized version");
        assertThat(result.snippet()).isNotEqualTo(longText); // 원본과 다름
        assertThat(result.snippet().length()).isLessThan(longText.length()); // 길이 축소됨

        // 다른 필드는 유지됨
        assertThat(result.id()).isEqualTo("doc-1");
        assertThat(result.title()).isEqualTo("Long Document");
        assertThat(result.url()).isEqualTo("https://example.com/doc-1");
        assertThat(result.score()).isEqualTo(0.95);

        // LLM 호출 확인
        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    @DisplayName("짧은 문서(2000자 미만)는 요약하지 않고 원본 그대로 반환")
    void short_document_not_summarized() {
        // given: 500자 짧은 문서
        String shortText = "Short content. ".repeat(35); // 약 525자
        RetrievedDocumentDto shortDoc = RetrievedDocumentDto.builder()
                .id("doc-2")
                .title("Short Document")
                .snippet(shortText)
                .url("https://example.com/doc-2")
                .score(0.80)
                .build();

        // ✅ 이 테스트는 모킹이 필요 없음 (LLM 호출 자체가 안 됨)

        // when: 요약 실행
        RetrievedDocumentDto result = service.summarizeIfNeeded(shortDoc);

        // then: 원본 그대로 반환됨
        assertThat(result).isEqualTo(shortDoc);
        assertThat(result.snippet()).isEqualTo(shortText);

        // LLM 호출 안 함
        verify(chatClient, never()).prompt(anyString());
    }

    @Test
    @DisplayName("요약 실패 시 원본 문서를 안전하게 반환")
    void summarization_failure_returns_original() {
        // given: 긴 문서
        String longText = "A".repeat(3000);
        RetrievedDocumentDto longDoc = RetrievedDocumentDto.builder()
                .id("doc-3")
                .title("Document")
                .snippet(longText)
                .url("https://example.com/doc-3")
                .score(0.90)
                .build();

        // ✅ 이 테스트에서만 모킹 설정 (예외 발생)
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenThrow(new RuntimeException("LLM API Error"));

        // when: 요약 실행
        RetrievedDocumentDto result = service.summarizeIfNeeded(longDoc);

        // then: 원본 문서가 안전하게 반환됨
        assertThat(result).isEqualTo(longDoc);
        assertThat(result.snippet()).isEqualTo(longText);

        // LLM 호출은 시도됨
        verify(chatClient, times(1)).prompt(anyString());
    }

    @Test
    @DisplayName("실제 긴 텍스트 요약 시나리오")
    void realistic_summarization_scenario() {
        // given: 실제처럼 긴 문서
        String realDocument = """
                OpenAI는 2015년에 설립된 인공지능 연구 기관입니다. 
                회사는 샘 알트먼, 일론 머스크 등이 공동 창립했으며, 
                초기에는 비영리 조직으로 시작했습니다.
                
                GPT(Generative Pre-trained Transformer) 시리즈를 개발하여 
                자연어 처리 분야에서 큰 혁신을 이뤘습니다. 
                특히 2022년 11월 출시된 ChatGPT는 전 세계적으로 큰 반향을 일으켰고,
                출시 5일 만에 100만 명의 사용자를 확보했습니다.
                
                """.repeat(20); // 2000자 이상으로 만듦

        RetrievedDocumentDto doc = RetrievedDocumentDto.builder()
                .id("openai-history")
                .title("OpenAI History")
                .snippet(realDocument)
                .url("https://example.com/openai")
                .score(0.98)
                .build();

        // ✅ 이 테스트에서만 모킹 설정
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        String summarized = "OpenAI는 2015년 설립된 AI 연구 기관으로, GPT 시리즈를 개발했으며 ChatGPT로 큰 성공을 거뒀습니다.";
        when(callResponseSpec.content()).thenReturn(summarized);

        // when: 요약 실행
        RetrievedDocumentDto result = service.summarizeIfNeeded(doc);

        // then: 핵심 정보가 보존된 짧은 요약본이 생성됨
        assertThat(result.snippet()).isEqualTo(summarized);
        assertThat(result.snippet().length()).isLessThan(realDocument.length());
        assertThat(result.id()).isEqualTo("openai-history");

        verify(chatClient, times(1)).prompt(anyString());
    }
}