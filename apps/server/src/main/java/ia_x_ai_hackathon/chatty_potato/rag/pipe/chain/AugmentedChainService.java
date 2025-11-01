package ia_x_ai_hackathon.chatty_potato.rag.pipe.chain;

import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto;
import ia_x_ai_hackathon.chatty_potato.rag.dto.AugmentedContextDto.Citation;
import ia_x_ai_hackathon.chatty_potato.rag.dto.RetrievedDocumentDto;
import ia_x_ai_hackathon.chatty_potato.rag.service.SummarizationService;
import ia_x_ai_hackathon.chatty_potato.rag.service.TokenAllocationStrategy;
import ia_x_ai_hackathon.chatty_potato.rag.service.TokenCounter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG 파이프라인의 "Augmentation" 단계 담당 서비스.
 *
 * <p>최적화 전략:
 * <ol>
 *   <li>토큰 제한 내 문서 선택 (탐욕 알고리즘)</li>
 *   <li>선택된 문서만 요약 (비용 절감)</li>
 *   <li>Context Text 조립</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AugmentedChainService {

    private final SummarizationService summarizationService;
    private final TokenAllocationStrategy tokenAllocationStrategy;

    /**
     * 검색된 문서들을 조립해 AugmentedContext 생성
     *
     * @param retrievedDocs 검색된 문서 리스트 (점수 순 정렬 가정)
     * @return AugmentedContextDto
     */
    public AugmentedContextDto assemble(List<RetrievedDocumentDto> retrievedDocs) {
        if (retrievedDocs == null || retrievedDocs.isEmpty()) return AugmentedContextDto.empty();

        var allocation = tokenAllocationStrategy.allocate();
        int maxContextTokens = allocation.context();

        var selected   = selectDocsGreedy(retrievedDocs, maxContextTokens);
        var summarized = summarizationService.summarizeBatch(selected);
        var trimmed    = hardCapByTokens(summarized, maxContextTokens);

        String contextText = buildContextText(trimmed);
        var citations = buildCitations(trimmed);

        int tokens = TokenCounter.count(contextText);
        log.info("Assembled context: {} docs, tokens={} (limit={})",
                trimmed.size(), tokens, maxContextTokens);

        return AugmentedContextDto.builder()
                .contextText(contextText)
                .citations(citations)
                .build();
    }

    /**
     * 요약 이후 실제 토큰 길이에 따라 context를 하드 컷하는 안전장치
     */
    private List<RetrievedDocumentDto> hardCapByTokens(List<RetrievedDocumentDto> docs, int maxTokens) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        List<RetrievedDocumentDto> result = new ArrayList<>();
        int totalTokens = 0;

        for (RetrievedDocumentDto doc : docs) {
            // snippet이 null일 수 있으니 방어
            String snippet = doc.snippet() == null ? "" : doc.snippet();
            int tokens = TokenCounter.count(snippet);

            if (totalTokens + tokens > maxTokens) {
                log.debug("hardCap stop: {} + {} > {}", totalTokens, tokens, maxTokens);
                break;
            }

            result.add(doc);
            totalTokens += tokens;
        }

        log.debug("hardCap result: {} docs ({} / {} tokens)", result.size(), totalTokens, maxTokens);
        return result;
    }

    /**
     * 탐욕 알고리즘으로 토큰 제한 내 최대한 많은 문서 선택
     *
     * <p>전략:
     * - 점수 순으로 정렬된 문서를 순회
     * - 긴 문서는 요약 시 절반으로 줄어든다고 가정
     * - 추정 토큰이 제한 내면 선택
     *
     * @param docs 검색된 문서 리스트
     * @param maxTokens 최대 토큰 수
     * @return 선택된 문서 리스트
     */
    private List<RetrievedDocumentDto> selectDocsGreedy(
            List<RetrievedDocumentDto> docs,
            int maxTokens
    ) {
        List<RetrievedDocumentDto> result = new ArrayList<>();
        int totalEstimatedTokens = 0;

        for (RetrievedDocumentDto doc : docs) {
            int docTokens = TokenCounter.count(doc.snippet());

            // 추정: 긴 문서(2000자 이상)는 요약 시 절반으로 줄어듦
            int estimatedTokens = docTokens > 1000 ? docTokens / 2 : docTokens;

            if (totalEstimatedTokens + estimatedTokens <= maxTokens) {
                result.add(doc);
                totalEstimatedTokens += estimatedTokens;

                log.debug("Selected doc[{}]: {} tokens (estimated: {})",
                        doc.id(), docTokens, estimatedTokens);
            } else {
                log.debug("Skipped doc[{}]: would exceed limit ({} + {} > {})",
                        doc.id(), totalEstimatedTokens, estimatedTokens, maxTokens);
                // 제한 초과 시 중단 (탐욕 알고리즘)
                break;
            }
        }

        log.info("Greedy selection: {} docs, estimated {} tokens (limit: {})",
                result.size(), totalEstimatedTokens, maxTokens);

        return result;
    }

    /**
     * 여러 RetrievedDocument의 snippet을 조립해
     * 모델에 주입할 contextText를 생성한다.
     */
    private String buildContextText(List<RetrievedDocumentDto> docs) {
        if (docs == null || docs.isEmpty()) {
            return "";
        }

        return docs.stream()
                .map(doc -> String.format("Source[%s]: %s", doc.id(), doc.snippet()))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * RetrievedDocument 리스트를 Citation 리스트로 변환한다.
     */
    private List<Citation> buildCitations(List<RetrievedDocumentDto> docs) {
        if (docs == null || docs.isEmpty()) {
            return List.of();
        }

        return docs.stream()
                .map(doc -> new Citation(
                        doc.id(),
                        doc.title(),
                        doc.score(),
                        doc.snippet()
                ))
                .toList();
    }
}