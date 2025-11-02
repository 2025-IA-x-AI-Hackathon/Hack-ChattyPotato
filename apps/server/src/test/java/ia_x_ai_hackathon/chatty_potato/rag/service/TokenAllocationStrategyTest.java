package ia_x_ai_hackathon.chatty_potato.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TokenAllocationStrategy (단일 정책) 테스트")
class TokenAllocationStrategyTest {

    private TokenAllocationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TokenAllocationStrategy();
    }

    @Test
    @DisplayName("✅ 단일 정책: 총 2200 입력 + 512 출력 할당")
    void single_policy_allocation() {
        var alloc = strategy.allocate();

        // 입력 구성
        assertThat(alloc.system()).isEqualTo(200);
        assertThat(alloc.rewrite()).isEqualTo(100);
        assertThat(alloc.format()).isEqualTo(200);
        assertThat(alloc.original()).isEqualTo(500);
        assertThat(alloc.context()).isEqualTo(1200);

        // 출력 제한
        assertThat(alloc.maxOutput()).isEqualTo(512);

        // 총합
        assertThat(alloc.totalInput()).isEqualTo(2200);

        System.out.println("✅ " + alloc);
    }

    @Test
    @DisplayName("✅ toString() 포맷 검증")
    void toString_format_check() {
        var alloc = strategy.allocate();
        String str = alloc.toString();
        assertThat(str).contains("TokenAllocation{");
        assertThat(str).contains("total=");
        assertThat(str).contains("maxOutput=");
    }

    @Test
    @DisplayName("✅ totalInput() 계산 정확성")
    void total_input_calculation() {
        var alloc = strategy.allocate();
        int manualSum = alloc.system() + alloc.rewrite() + alloc.format() + alloc.original() + alloc.context();
        assertThat(alloc.totalInput()).isEqualTo(manualSum);
    }
}
