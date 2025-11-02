package ia_x_ai_hackathon.chatty_potato.rag.controller;

import ia_x_ai_hackathon.chatty_potato.common.resolver.UserId;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RagResultDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RewriteReqDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RewriteResDto;
import ia_x_ai_hackathon.chatty_potato.rag.pipe.RagPipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RAGController {

	private final RagPipelineService ragPipelineService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RewriteResDto rewriteQuery(@UserId String userId, @RequestBody RewriteReqDto rewriteReqDto) {
		return ragPipelineService.rewriteQuery(userId, rewriteReqDto.query());
	}

	@PostMapping("/route")
	@ResponseStatus(HttpStatus.OK)
	public RagResultDto high(
			@UserId String userId,
			@RequestParam String taskId,
			@RequestParam(defaultValue = "10000") long waitMillis,
			@RequestParam(defaultValue = "200") long stepMillis
	) {
		return ragPipelineService.produce(userId, taskId, waitMillis, stepMillis);
	}

}
