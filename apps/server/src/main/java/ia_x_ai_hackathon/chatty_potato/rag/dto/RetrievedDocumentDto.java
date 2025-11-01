package ia_x_ai_hackathon.chatty_potato.rag.dto;

import lombok.Builder;

@Builder
public record RetrievedDocumentDto(
        String id,
        String title,
        String snippet,
        String url,
        double score
) {
    @Override
    public String toString() {
        String sn = snippet == null ? "" :
                (snippet.length() > 120 ? snippet.substring(0, 120) + "..." : snippet);
        return "Doc{id='%s', score=%.3f, title='%s', snip='%s'}"
                .formatted(id, score, title, sn);
    }
}
