package com.teamsync.back.archive.file;

import com.teamsync.back.common.BaseTimeEntity;
import com.teamsync.back.project.Project;
import com.teamsync.back.user.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-204 파일 아카이브. 실제 바이트는 로컬 디스크(후속 S3 교체 가능, com.teamsync.back.storage 참고)에
 * 저장하고, 이 엔티티는 메타데이터만 보관한다. storageKey는 서버가 생성한 UUID 기반 내부 파일명으로,
 * originalFilename을 디스크 경로에 그대로 쓰지 않아 경로 순회/파일명 충돌을 방지한다(PRD 5.6, 3.6 보안).
 */
@Getter
@Entity
@Table(name = "archived_files")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArchivedFile extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "content_type", length = 255)
	private String contentType;

	@Column(name = "size_bytes", nullable = false)
	private long sizeBytes;

	@Column(name = "storage_key", nullable = false, unique = true, length = 255)
	private String storageKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploader_id", nullable = false)
	private User uploader;

	@ElementCollection
	@CollectionTable(name = "archived_file_tags", joinColumns = @JoinColumn(name = "archived_file_id"))
	@OrderColumn(name = "position")
	@Column(name = "tag", nullable = false, length = 100)
	private List<String> tags = new ArrayList<>();

	public ArchivedFile(Project project, String originalFilename, String contentType, long sizeBytes,
			String storageKey, User uploader, List<String> tags) {
		this.project = project;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.sizeBytes = sizeBytes;
		this.storageKey = storageKey;
		this.uploader = uploader;
		this.tags = new ArrayList<>(tags);
	}
}
