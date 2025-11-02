package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.PromptAssemblyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeneratorChainService {

    private final ChatClient chatClient;

    /**
     * 조립된 context와 사용자 질의를 기반으로 답변 생성
     */
    public PromptAssemblyDto generatePrompt(String userQuery,
                                            String rewritePrompt,
                                            AugmentedContextDto context) {

        String systemPrompt = """
                You are an intelligent assistant specialized in retrieval-augmented generation (RAG).
                Always answer using the CONTEXT below when relevant.
                Avoid hallucinations. Cite the document titles where possible.
                Format answers in markdown for readability.
                Please respond in Korean.
                """;

        // 🧩 프롬프트 조립
        String userPrompt = "User question:\n" + userQuery;
        String contextText = context.contextText();

        String finalPrompt = """
                [System]
                %s

                [Rewritten Query]
                %s

                [Retrieved Context]
                %s

                [User Input]
                %s
                """.formatted(systemPrompt, rewritePrompt, contextText, userPrompt);

        log.debug("🧠 Final prompt assembled ({} chars)", finalPrompt.length());

        return new PromptAssemblyDto(
                systemPrompt,
                userPrompt,
                rewritePrompt,
                contextText,
                finalPrompt
        );
    }

    /**
     * 실제 LLM 호출 수행
     */
    public String generateAnswer(PromptAssemblyDto prompt) {
        try {
            var response = chatClient.prompt(prompt.finalPrompt()).call();
            String content = response.content();
            log.info("✅ Generation complete ({} chars)", content.length());
            return content;
        } catch (Exception e) {
            log.error("❌ LLM generation failed: {}", e.getMessage(), e);
            return "⚠️ Sorry, an internal generation error occurred.";
        }
    }
}
