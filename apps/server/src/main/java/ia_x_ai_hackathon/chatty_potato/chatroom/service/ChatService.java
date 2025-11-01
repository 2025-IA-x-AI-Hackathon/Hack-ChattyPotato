package ia_x_ai_hackathon.chatty_potato.chatroom.service;

import ia_x_ai_hackathon.chatty_potato.chatroom.document.ChatMessage;
import ia_x_ai_hackathon.chatty_potato.chatroom.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

	private final ChatMessageRepository chatMessageRepository;

	public String createChatRoom() {
		return UUID.randomUUID().toString();
	}

	public void saveMessage(String chatRoomId, String userId, String message, ChatMessage.Sender sender) {
		ChatMessage chatMessage = ChatMessage.builder()
				.chatRoomId(chatRoomId)
				.userId(userId)
				.message(message)
				.sender(sender)
				.timestamp(LocalDateTime.now())
				.build();
		chatMessageRepository.save(chatMessage);
	}

	public List<ChatMessage> getChatHistory(String chatRoomId) {
		return chatMessageRepository.findByChatRoomId(chatRoomId);
	}

}
