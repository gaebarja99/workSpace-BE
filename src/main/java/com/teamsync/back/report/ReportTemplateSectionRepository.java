package com.teamsync.back.report;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTemplateSectionRepository extends JpaRepository<ReportTemplateSection, Long> {

	List<ReportTemplateSection> findAllByTemplate_IdOrderByOrderIndexAsc(Long templateId);

	boolean existsByTemplate_IdAndSectionKey(Long templateId, ReportSectionKey sectionKey);

	Optional<ReportTemplateSection> findByIdAndTemplate_Id(Long id, Long templateId);

	long countByTemplate_Id(Long templateId);
}
