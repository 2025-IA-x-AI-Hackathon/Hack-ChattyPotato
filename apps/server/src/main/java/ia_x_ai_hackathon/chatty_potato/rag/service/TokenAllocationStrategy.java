package ia_x_ai_hackathon.chatty_potato.rag.service;

import org.springframework.stereotype.Component;

/**
 * 단일 토큰 할당 전략 (입력 예산 고정)
 * 총 입력 예산: 2200 (system+rewrite+format+original+context)
 * 출력 최대: 512
 */
@Component
public class TokenAllocationStrategy {

    // 고정 섹션
    private static final int SYSTEM_PROMPT = 200;
    private static final int REWRITE_QUERY = 100;
    private static final int OUTPUT_FORMAT = 200;

    // 가변 섹션(이제 단일값로 확정)
    private static final int ORIGINAL = 500;
    private static final int CONTEXT  = 1200; // 여유 있게 운영하려면 1200 권장

    // 출력 제한(고정)
    private static final int MAX_OUTPUT = 512;

    /** 단일 정책 할당 */
    public TokenAllocation allocate() {
        return new TokenAllocation(
                SYSTEM_PROMPT,
                REWRITE_QUERY,
                OUTPUT_FORMAT,
                ORIGINAL,
                CONTEXT,
                MAX_OUTPUT
        );
    }

    public record TokenAllocation(
            int system, int rewrite, int format, int original, int context, int maxOutput
    ) {
        public int totalInput() {
            return system + rewrite + format + original + context;
        }

        @Override
        public String toString() {
            return "TokenAllocation{total=%d, system=%d, rewrite=%d, format=%d, original=%d, context=%d, maxOutput=%d}"
                    .formatted(totalInput(), system, rewrite, format, original, context, maxOutput);
        }
    }
}
