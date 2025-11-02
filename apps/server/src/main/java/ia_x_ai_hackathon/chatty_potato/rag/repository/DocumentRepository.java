package ia_x_ai_hackathon.chatty_potato.rag.repository;

import ia_x_ai_hackathon.chatty_potato.rag.entity.DocumentEntity;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentEntity, String> {
	// 기본 CRUD는 자동 제공 (save, findById, findAll, delete 등)

	// 커스텀 쿼리 메서드 추가 가능
	List<DocumentEntity> findByTitle(String title);
	List<DocumentEntity> findByContentContaining(String keyword);
}
