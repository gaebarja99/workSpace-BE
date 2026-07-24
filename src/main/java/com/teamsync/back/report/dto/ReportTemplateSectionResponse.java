package com.teamsync.back.report.dto;

import com.teamsync.back.report.ReportSectionKey;
import com.teamsync.back.report.ReportTemplateSection;

/**
 * id는 SYSTEM_DEFAULT(비영속 가상 템플릿)의 섹션인 경우에만 null이다.
 */
public record ReportTemplateSectionResponse(
		Long id,
		ReportSectionKey sectionKey,
		String title,
		int orderIndex,
		boolean autoFilled
) {
	public static ReportTemplateSectionResponse from(ReportTemplateSection section) {
		return new ReportTemplateSectionResponse(section.getId(), section.getSectionKey(), section.getTitle(),
				section.getOrderIndex(), section.isAutoFilled());
	}
}
