package ia_x_ai_hackathon.chatty_potato.rag.dto;

public record PromptAssemblyDto(
        String systemPrompt,
        String userPrompt,
        String rewritePrompt,
        String contextText,
        String finalPrompt
) {
    @Override
    public String toString() {
        return "Prompt{sys=%d, user=%d, ctx=%d, final=%d}"
                .formatted(len(systemPrompt), len(userPrompt),
                        len(contextText), len(finalPrompt));
    }

    private int len(String s) {
        return s == null ? 0 : s.length();
    }
}