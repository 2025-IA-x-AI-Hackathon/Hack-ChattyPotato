package ia_x_ai_hackathon.chatty_potato.rag.exception;

import lombok.Getter;

@Getter
public class TaskNotFoundException extends RuntimeException {
	private final String userId, taskId;
	public TaskNotFoundException(String userId, String taskId) {
		super("Task not found");
		this.userId = userId; this.taskId = taskId;
	}
}
