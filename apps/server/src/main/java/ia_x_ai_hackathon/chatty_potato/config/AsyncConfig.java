package ia_x_ai_hackathon.chatty_potato.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
	@Bean(name = "ragExecutor")
	public Executor ragExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(4);
		ex.setMaxPoolSize(8);
		ex.setQueueCapacity(100);
		ex.setThreadNamePrefix("rag-async-");
		ex.initialize();
		return ex;
	}

}
