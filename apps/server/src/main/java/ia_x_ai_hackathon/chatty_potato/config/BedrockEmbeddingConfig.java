package ia_x_ai_hackathon.chatty_potato.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.bedrock.titan.BedrockTitanEmbeddingModel;
import org.springframework.ai.bedrock.titan.api.TitanEmbeddingBedrockApi;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;

@Configuration
public class BedrockEmbeddingConfig {

    @Value("${spring.ai.bedrock.aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public EmbeddingModel bedrockTitanEmbeddingModel(ObservationRegistry observationRegistry) {
        // 1️⃣ Titan 모델 ID
        String modelId = TitanEmbeddingBedrockApi.TitanEmbeddingModel.TITAN_EMBED_TEXT_V1.id();

        // 2️⃣ AWS 자격 증명
        AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();

        // 3️⃣ Region
        Region region = Region.of(awsRegion);

        // 4️⃣ ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        // 5️⃣ Timeout
        Duration timeout = Duration.ofSeconds(30);

        // 6️⃣ Titan API 인스턴스 생성
        TitanEmbeddingBedrockApi titanApi = new TitanEmbeddingBedrockApi(
                modelId,
                credentialsProvider,
                region,
                objectMapper,
                timeout
        );

        // 7️⃣ ObservationRegistry 주입 (Spring Boot 자동 Bean)
        return new BedrockTitanEmbeddingModel(titanApi, observationRegistry);
    }
}
