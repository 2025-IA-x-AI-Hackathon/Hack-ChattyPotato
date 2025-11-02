package ia_x_ai_hackathon.chatty_potato.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Configuration
public class OpenAIEmbeddingConfig {

	@Value("${spring.ai.openai.api-key}")
	private String openAiApiKey;

	@Bean
	@Primary
	public EmbeddingModel primaryOpenAiEmbeddingModel(
			@Qualifier("openAiEmbeddingModel") EmbeddingModel delegate) {
		return delegate; // 기본 임베딩 = OpenAI
	}
	
}