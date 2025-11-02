package ia_x_ai_hackathon.chatty_potato.common.exception;

import ia_x_ai_hackathon.chatty_potato.rag.exception.PromptBuildFailedException;
import ia_x_ai_hackathon.chatty_potato.rag.exception.PromptTimeoutException;
import ia_x_ai_hackathon.chatty_potato.rag.exception.TaskNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(PromptTimeoutException.class)
	public ResponseEntity<ProblemDetail> handle(PromptTimeoutException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.GATEWAY_TIMEOUT); // 504
		pd.setTitle("Prompt Timeout");
		pd.setDetail(ex.getMessage());
		pd.setProperty("userId", ex.getUserId());
		pd.setProperty("taskId", ex.getTaskId());
		pd.setProperty("waitedMillis", ex.getWaitedMillis());

		var headers = new HttpHeaders();
		headers.add(HttpHeaders.RETRY_AFTER, "2"); // 2초 후 재시도 권장 (원하는 값으로)
		return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).headers(headers).body(pd);
	}


	@ExceptionHandler(TaskNotFoundException.class)
	public ResponseEntity<ProblemDetail> handle(TaskNotFoundException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
		pd.setTitle("Task Not Found");
		pd.setDetail(ex.getMessage());
		pd.setProperty("userId", ex.getUserId());
		pd.setProperty("taskId", ex.getTaskId());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
	}

	@ExceptionHandler(PromptBuildFailedException.class)
	public ResponseEntity<ProblemDetail> handle(PromptBuildFailedException ex) {
		var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
		pd.setTitle("Prompt Build Failed");
		pd.setDetail(ex.getMessage());
		pd.setProperty("userId", ex.getUserId());
		pd.setProperty("taskId", ex.getTaskId());
		return ResponseEntity.badRequest().body(pd);
	}

}
