package ia_x_ai_hackathon.chatty_potato.rag.exception;

public class TimeoutException extends RuntimeException {
	public TimeoutException(String message) {
		super(message);
	}
}
