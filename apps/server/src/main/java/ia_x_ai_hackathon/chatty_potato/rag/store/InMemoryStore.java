package ia_x_ai_hackathon.chatty_potato.rag.store;


import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class InMemoryStore {

	public enum Status { PENDING, READY, ERROR }

	@Getter
	@Setter
	@Builder
	public static class Slot {
		private String userId;
		private String taskId;

		private String original;
		private String rewritten;
		private AugmentedContextDto augmentedContext;

		// 프롬프트 준비 완료/실패 신호
		private CompletableFuture<Object> promptFuture;

		private volatile String error; // 실패 메시지 저장
		private volatile Status status;

		private Instant createdAt;
		private AtomicBoolean buildStarted; // 중복 방지
	}

	private final ConcurrentHashMap<String, Slot> map = new ConcurrentHashMap<>();

	private String key(String userId, String taskId) {
		return userId + ":" + taskId;
	}

	/** rewrite 직후 초기화 (PENDING) */
	public void init(String userId, String taskId, String original, String rewritten) {
		Slot slot = Slot.builder()
				.userId(userId)
				.taskId(taskId)
				.original(original)
				.rewritten(rewritten)
				.promptFuture(new CompletableFuture<>())
				.status(Status.PENDING)
				.createdAt(Instant.now())
				.buildStarted(new AtomicBoolean(false))
				.build();
		map.put(key(userId, taskId), slot);
	}

	/** 중복 시작 방지: 최초 1회만 true */
	public boolean markBuildStarted(String userId, String taskId) {
		Slot s = map.get(key(userId, taskId));
		return s != null && s.getBuildStarted().compareAndSet(false, true);
	}

	public void completePrompt(String userId, String taskId, Object promptDto, AugmentedContextDto augmentedContext) {
		Slot s = map.get(key(userId, taskId));
		if (s == null) return;
		s.setStatus(Status.READY);
		s.getPromptFuture().complete(promptDto);
		s.setAugmentedContext(augmentedContext);
	}

	public void failPrompt(String userId, String taskId, String message) {
		Slot s = map.get(key(userId, taskId));
		if (s == null) return;
		s.setError(message);
		s.setStatus(Status.ERROR);
		s.getPromptFuture().completeExceptionally(new RuntimeException(message));
	}

	public Optional<Slot> get(String userId, String taskId) {
		return Optional.ofNullable(map.get(key(userId, taskId)));
	}

	public Optional<String> getRewritten(String userId, String taskId) {
		return get(userId, taskId).map(Slot::getRewritten);
	}
}