package ia_x_ai_hackathon.chatty_potato.rag.dto;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record AugmentedContextDto(
        String contextText,
        List<Citation> citations
) {
    @Builder
    public record Citation(
            String id,
            String title,
            double score,
            String snippet
    ) {
        @Override
        public String toString() {
            String sn = snippet == null ? "" :
                    (snippet.length() > 60 ? snippet.substring(0, 60) + "..." : snippet);
            return "Citation{id='%s', score=%.2f, title='%s', snip='%s'}"
                    .formatted(id, score, title, sn);
        }
    }

    public static AugmentedContextDto empty() {
        return new AugmentedContextDto("", List.of());
    }

    public boolean hasContext() {
        return contextText != null && !contextText.isEmpty();
    }

    @Override
    public String toString() {
        String cites = citations == null ? "[]" :
                citations.stream()
                        .map(Citation::toString)
                        .collect(Collectors.joining(",\n  "));
        String ctx = contextText == null ? "" :
                (contextText.length() > 200 ? contextText.substring(0, 200) + "..." : contextText);
        return "AugmentedContext{\n  ctx='%s',\n  cites=[%s]\n}"
                .formatted(ctx, cites);
    }
}