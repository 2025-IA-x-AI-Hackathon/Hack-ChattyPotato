package ia_x_ai_hackathon.chatty_potato.rag.dto;

import java.util.List;

public record LLMResponseDto(
        String raw,
        String summary,
        List<String> sources
) {
    // 필요시 커스텀 생성자
    public LLMResponseDto {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}