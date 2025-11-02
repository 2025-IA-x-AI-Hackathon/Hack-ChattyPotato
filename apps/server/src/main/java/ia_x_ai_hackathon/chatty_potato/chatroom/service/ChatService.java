package ia_x_ai_hackathon.chatty_potato.chatroom.service;

import ia_x_ai_hackathon.chatty_potato.chatroom.document.ChatMessage;
import ia_x_ai_hackathon.chatty_potato.chatroom.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
//
//package ia_x_ai_hackathon.chatty_potato.chatroom.service;
//
//import ia_x_ai_hackathon.chatty_potato.chatroom.document.ChatMessage;
//import ia_x_ai_hackathon.chatty_potato.chatroom.repository.ChatMessageRepository;
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import lombok.RequiredArgsConstructor;
//import org.springframework.ai.bedrock.anthropic.BedrockAnthropicChatClient;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class ChatService {
//
//	private final ChatMessageRepository chatMessageRepository;
//	private final BedrockAnthrockChatClient ohioChatClient;
//	private final RedisTemplate<String, Object> redisTemplate;
//
//	public Long createChatRoom(String userId) {
//		return System.currentTimeMillis();
//	}
//
//	public void saveMessage(Long chatRoomId, String userId, String message, ChatMessage.Sender sender) {
//		ChatMessage chatMessage = ChatMessage.builder()
//				.chatRoomId(chatRoomId)
//				.userId(userId)
//				.message(message)
//				.sender(sender)
//				.timestamp(LocalDateTime.now())
//				.build();
//		chatMessageRepository.save(chatMessage);
//	}
//
//	public List<ChatMessage> getChatHistory(Long chatRoomId) {
//		return chatMessageRepository.findByChatRoomId(chatRoomId);
//	}
//
//	@Async
//	public void generateAndCachePrompt(String query, List<ChatMessage> chatHistory) {
//		String prompt = buildPrompt(query, chatHistory);
//		redisTemplate.opsForValue().set(query, prompt, 1, TimeUnit.HOURS);
//	}
//
//	public String getCachedPrompt(String query) {
//		return (String) redisTemplate.opsForValue().get(query);
//	}
//
//	public String getAiResponse(String prompt) {
//		return ohioChatClient.call(prompt);
//	}
//
//	private String buildPrompt(String query, List<ChatMessage> chatHistory) {
//		// You can customize this part to build a prompt with the chat history
//		String prompt = query;
//		for (ChatMessage chatMessage : chatHistory) {
//			prompt = chatMessage.getSender() + ": " + chatMessage.getMessage() + "\n" + prompt;
//		}
//		return prompt;
//	}
//}
