package ia_x_ai_hackathon.chatty_potato.chatroom.controller;

import ia_x_ai_hackathon.chatty_potato.chatroom.document.ChatMessage;
import ia_x_ai_hackathon.chatty_potato.chatroom.dto.ChatRequest;
import ia_x_ai_hackathon.chatty_potato.chatroom.service.ChatService;
import ia_x_ai_hackathon.chatty_potato.common.resolver.UserId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

	@PostMapping("/room")
	@ResponseStatus(HttpStatus.CREATED)
	public String createChatRoom() {
		return chatService.createChatRoom();
	}

	@GetMapping("/{chatRoomId}/history")
	public List<ChatMessage> getChatHistory(@PathVariable String chatRoomId) {
		return chatService.getChatHistory(chatRoomId);
	}
}
