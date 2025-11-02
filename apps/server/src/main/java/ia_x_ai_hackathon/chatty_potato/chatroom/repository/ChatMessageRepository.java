package ia_x_ai_hackathon.chatty_potato.chatroom.repository;

import ia_x_ai_hackathon.chatty_potato.chatroom.document.ChatMessage;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ChatMessageRepository extends ElasticsearchRepository<ChatMessage, String> {

    List<ChatMessage> findByChatRoomId(String chatRoomId);
}
