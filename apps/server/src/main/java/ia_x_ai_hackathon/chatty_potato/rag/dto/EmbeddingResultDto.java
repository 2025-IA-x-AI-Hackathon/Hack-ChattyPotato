package ia_x_ai_hackathon.chatty_potato.rag.dto;

public record EmbeddingResultDto(
        String id,
        float[] vector,
        int dims
) {
    // ⚠️ float[] 은 mutable이므로 방어적 복사 권장
    public EmbeddingResultDto {
        vector = vector == null ? new float[0] : vector.clone();
    }

    // getter override로 방어적 복사
    @Override
    public float[] vector() {
        return vector.clone();
    }
}