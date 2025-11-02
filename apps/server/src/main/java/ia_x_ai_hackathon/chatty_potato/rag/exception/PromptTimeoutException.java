package ia_x_ai_hackathon.chatty_potato.rag.exception;

import lombok.Getter;

@Getter
public class PromptTimeoutException extends RuntimeException {
	private final String userId;
	private final String taskId;
	private final long waitedMillis;

	public PromptTimeoutException(String userId, String taskId, long waitedMillis) {
		super("Prompt not ready within " + waitedMillis + " ms");
		this.userId = userId;
		this.taskId = taskId;
		this.waitedMillis = waitedMillis;
	}
}
