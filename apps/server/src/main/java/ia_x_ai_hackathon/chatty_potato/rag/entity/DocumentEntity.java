package ia_x_ai_hackathon.chatty_potato.rag.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "documents")
@Getter @Builder
@NoArgsConstructor @AllArgsConstructor
public class DocumentEntity {

    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Keyword)
    private String url;

    // ES 매핑은 인덱스 생성시 index/similarity 옵션까지 반드시 지정하세요.
    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private float[] embedding;
}
