package ia_x_ai_hackathon.chatty_potato.rag.dto;

import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto.Citation;
import java.time.Instant;
import java.util.List;

/**
 * 결과 DTO 정의
 */
public record RagResultDto(
        String sessionId,
        String originalQuery,
        String rewrittenQuery,
        String answer,
        PromptAssemblyDto prompt,
        List<Citation> citations,
        Instant createdAt
) {
    public static RagResultDto failed(String session, String query, String errorMsg) {
        return new RagResultDto(session, query, null,
                "⚠️ Internal Error: " + errorMsg, null, List.of(), Instant.now());
    }
}
