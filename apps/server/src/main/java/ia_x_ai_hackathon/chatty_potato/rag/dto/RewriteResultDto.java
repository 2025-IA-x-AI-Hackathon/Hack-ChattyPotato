package ia_x_ai_hackathon.chatty_potato.rag.dto;

import lombok.Builder;

@Builder
public record RewriteResultDto(
        String originalQuery,
        String rewrittenQuery,
        int originalTokens,
        int rewrittenTokens
) {
    @Override
    public String toString() {
        return "Rewrite{origTok=%d, reTok=%d, rewritten='%s'}"
                .formatted(originalTokens, rewrittenTokens, rewrittenQuery);
    }
}
