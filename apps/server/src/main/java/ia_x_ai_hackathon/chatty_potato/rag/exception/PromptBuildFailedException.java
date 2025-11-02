package ia_x_ai_hackathon.chatty_potato.rag.exception;

import lombok.Getter;

@Getter
public class PromptBuildFailedException extends RuntimeException {
	private final String userId, taskId;
	public PromptBuildFailedException(String userId, String taskId, String message) {
		super(message);
		this.userId = userId; this.taskId = taskId;
	}
}
