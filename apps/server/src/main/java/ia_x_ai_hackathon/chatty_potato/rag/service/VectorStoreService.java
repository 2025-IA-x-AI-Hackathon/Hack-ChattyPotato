package ia_x_ai_hackathon.chatty_potato.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

	private final VectorStore vectorStore;

	/** 단일 텍스트 저장 (자동 임베딩) */
	public String save(String content, Map<String, Object> metadata) {
		Document doc = (metadata == null) ? new Document(content) : new Document(content, metadata);
		vectorStore.add(List.of(doc)); // 자동 임베딩 + 인덱싱
		log.info("Indexed doc: id={}, metadataKeys={}", doc.getId(), doc.getMetadata().keySet());
		return doc.getId();
	}

	/** externalId 기준 업서트(동일 externalId 모두 삭제 후 재색인) */
	public String upsertByExternalId(String externalId, String content, Map<String, Object> metadata) {
		if (externalId == null || externalId.isBlank()) {
			throw new IllegalArgumentException("externalId must not be null/blank");
		}

		// 1) 같은 externalId를 가진 기존 문서 삭제
		var b = new FilterExpressionBuilder();
		vectorStore.delete(b.eq("externalId", externalId).build());

		// 2) 새 문서 추가 (externalId를 메타에 넣음)
		Map<String, Object> meta = new HashMap<>(metadata == null ? Map.of() : metadata);
		meta.put("externalId", externalId);

		Document doc = new Document(content, meta);
		vectorStore.add(List.of(doc));
		log.info("Upserted doc by externalId={}, newId={}", externalId, doc.getId());
		return doc.getId();
	}

	/** 배치 업서트 */
	public List<String> upsertAllByExternalId(List<DocInput> inputs) {
		if (inputs == null || inputs.isEmpty()) return List.of();

		// 먼저 모두 지우기
		var b = new FilterExpressionBuilder();
		inputs.stream()
				.map(DocInput::externalId)
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.forEach(id -> vectorStore.delete(b.eq("externalId", id).build()));

		// 그리고 새로 추가
		List<Document> docs = new ArrayList<>();
		for (DocInput in : inputs) {
			Map<String, Object> meta = new HashMap<>(in.metadata() == null ? Map.of() : in.metadata());
			if (in.externalId() != null && !in.externalId().isBlank()) {
				meta.put("externalId", in.externalId());
			}
			docs.add(new Document(in.content(), meta));
		}
		vectorStore.add(docs);
		return docs.stream().map(Document::getId).toList();
	}

	/** KNN 검색 */
	public List<Document> search(String query, int topK) {
		return vectorStore.similaritySearch(SearchRequest.builder()
				.query(query)
				.topK(topK)
				.build());
	}

	/** 메타데이터 externalId 기준 삭제 */
	public void deleteByExternalId(String externalId) {
		var b = new FilterExpressionBuilder();
		vectorStore.delete(b.eq("externalId", externalId).build());
	}

	/** 입력 DTO */
	public record DocInput(String externalId, String content, Map<String, Object> metadata) {}
}