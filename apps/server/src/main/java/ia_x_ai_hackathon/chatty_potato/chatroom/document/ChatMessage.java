package ia_x_ai_hackathon.chatty_potato.chatroom.document;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "chat-history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    private String id;

    private String chatRoomId;

    private String userId;

    private String message;

    private Sender sender;

    private LocalDateTime timestamp;

    public enum Sender {
        USER, AI
    }
}
