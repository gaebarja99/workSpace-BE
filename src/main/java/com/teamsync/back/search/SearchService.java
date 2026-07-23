package com.teamsync.back.search;

import com.teamsync.back.archive.file.ArchivedFileRepository;
import com.teamsync.back.auth.AuthenticatedUser;
import com.teamsync.back.channel.message.MessageRepository;
import com.teamsync.back.channel.message.MessageType;
import com.teamsync.back.common.exception.InvalidSearchRequestException;
import com.teamsync.back.search.dto.SearchFileResult;
import com.teamsync.back.search.dto.SearchMessageResult;
import com.teamsync.back.search.dto.SearchResponse;
import com.teamsync.back.search.dto.SearchTaskResult;
import com.teamsync.back.search.dto.SearchUserResult;
import com.teamsync.back.task.TaskRepository;
import com.teamsync.back.user.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-004(워크스페이스 전체 통합 검색): 태스크/메시지/파일/사용자를 하나의 검색어로 조회한다.
 * PRD 5.3이 이상향으로 제시하는 Elasticsearch/OpenSearch는 이번 스코프에 포함하지 않고,
 * 각 Repository의 JPA LIKE(ContainingIgnoreCase 상당) 파생/@Query 조회로 카테고리당 상위 10건만 반환하는
 * MVP로 구현한다(현재 코드베이스에 검색 인프라가 전무하고, PRD 5.1의 "초기부터 과도한 복잡도 지양" 원칙에 따름).
 * 모든 조회는 principal.workspaceId() 기준으로 스코핑되어 다른 워크스페이스 데이터는 조회되지 않는다(PRD 5.6 리스크 대응).
 */
@Service
public class SearchService {

	private static final int RESULT_LIMIT = 10;

	private final TaskRepository taskRepository;
	private final MessageRepository messageRepository;
	private final ArchivedFileRepository archivedFileRepository;
	private final UserRepository userRepository;

	public SearchService(TaskRepository taskRepository, MessageRepository messageRepository,
			ArchivedFileRepository archivedFileRepository, UserRepository userRepository) {
		this.taskRepository = taskRepository;
		this.messageRepository = messageRepository;
		this.archivedFileRepository = archivedFileRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public SearchResponse search(AuthenticatedUser principal, String rawQuery) {
		String query = rawQuery == null ? "" : rawQuery.trim();
		if (query.isEmpty()) {
			throw new InvalidSearchRequestException("검색어(q)는 비어 있을 수 없습니다.");
		}

		Long workspaceId = principal.workspaceId();
		Pageable limit = PageRequest.of(0, RESULT_LIMIT);
		String keyword = escapeLikeWildcards(query);

		var tasks = taskRepository.searchByWorkspace(workspaceId, keyword, limit).stream()
				.map(SearchTaskResult::from)
				.toList();
		var messages = messageRepository.searchByWorkspace(workspaceId, keyword, MessageType.SYSTEM, limit).stream()
				.map(SearchMessageResult::from)
				.toList();
		var files = archivedFileRepository.searchByWorkspace(workspaceId, keyword, limit).stream()
				.map(SearchFileResult::from)
				.toList();
		var users = userRepository.searchByWorkspace(workspaceId, keyword, limit).stream()
				.map(SearchUserResult::from)
				.toList();

		return new SearchResponse(query, tasks, messages, files, users);
	}

	// LIKE 패턴의 와일드카드 문자(%, _)를 이스케이프해, 사용자가 "%"나 "_"를 검색어로 입력했을 때
	// 의도치 않게 모든 행이 매칭되는 것을 방지한다(각 Repository 쿼리는 ESCAPE '\'를 지정해 이를 해석한다).
	private static String escapeLikeWildcards(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
