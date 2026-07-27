package com.teamsync.back.report;

import com.teamsync.back.report.dto.EntryResponse;
import com.teamsync.back.report.dto.ExecutiveCategoryGroup;
import com.teamsync.back.report.dto.ExecutiveDashboardResponse;
import com.teamsync.back.report.dto.ExecutiveMemberEntries;
import com.teamsync.back.report.dto.MemberSubmissionStatus;
import com.teamsync.back.report.dto.TeamDashboardResponse;
import com.teamsync.back.report.dto.TeamMemberReportEntries;
import com.teamsync.back.report.dto.TeamWeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportExportView;
import com.teamsync.back.report.dto.WeeklyReportResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * FR-409(xlsx 내보내기): 대/중/소분류 + 상세업무 + 달성율(%) 표 형식(캡처 원본 엑셀 레이아웃)을
 * Apache POI로 재현하는 순수 빌더. PDF와 동일하게 "요청 시 즉석 렌더링, 저장 안 함" 원칙을 따른다
 * (ReportExcelExportService가 호출, byte[]를 즉시 반환).
 */
final class ReportExcelBuilder {

	private static final String[] COLUMNS = {"대분류", "중분류", "소분류", "상세업무", "달성율(%)"};
	private static final int[] COLUMN_WIDTHS = {5000, 4500, 5000, 12000, 2500};
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private ReportExcelBuilder() {
	}

	static byte[] buildIndividualWorkbook(WeeklyReportExportView view) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			WeeklyReportResponse r = view.report();
			Sheet sheet = workbook.createSheet("주간보고");
			Styles styles = new Styles(workbook);
			int rowIndex = 0;
			rowIndex = writeTitleRow(sheet, styles, rowIndex,
					view.authorName() + " 주간 보고 (" + r.weekStart().format(DATE) + " ~ " + r.weekEnd().format(DATE) + ")");
			rowIndex++;
			rowIndex = writeEntrySection(sheet, styles, rowIndex, "금주 진행사항", r.entries().thisWeek());
			rowIndex++;
			writeEntrySection(sheet, styles, rowIndex, "차주 진행사항", r.entries().nextWeek());
			applyColumnWidths(sheet);
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("주간 보고 xlsx 생성에 실패했습니다.", e);
		}
	}

	static byte[] buildTeamWorkbook(TeamWeeklyReportExportView view) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			TeamDashboardResponse r = view.report();
			Sheet sheet = workbook.createSheet("팀 주간보고");
			Styles styles = new Styles(workbook);
			int rowIndex = writeTitleRow(sheet, styles, 0,
					"팀 주간 보고 (" + r.weekStart().format(DATE) + " ~ " + r.weekEnd().format(DATE) + ") · 제출 "
							+ r.submittedCount() + "/" + r.totalMemberCount());
			rowIndex++;

			for (TeamMemberReportEntries member : r.members()) {
				rowIndex = writeTitleRow(sheet, styles, rowIndex, member.name() + " ("
						+ (member.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출") + ")");
				rowIndex = writeEntrySection(sheet, styles, rowIndex, "금주 진행사항", member.thisWeekEntries());
				rowIndex++;
				rowIndex = writeEntrySection(sheet, styles, rowIndex, "차주 진행사항", member.nextWeekEntries());
				rowIndex++;
			}
			applyColumnWidths(sheet);
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("팀 주간 보고 xlsx 생성에 실패했습니다.", e);
		}
	}

	/** 대표 뷰: 대분류(프로젝트)별 그룹 구조 — 대분류 헤더 아래 멤버별로 해당 대분류 항목만 나열한다. */
	static byte[] buildExecutiveWorkbook(ExecutiveDashboardResponse view) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("대표 뷰");
			Styles styles = new Styles(workbook);
			int rowIndex = writeTitleRow(sheet, styles, 0,
					"대표 뷰 (" + view.weekStart().format(DATE) + " ~ " + view.weekEnd().format(DATE) + ")");
			rowIndex++;

			for (ExecutiveCategoryGroup category : view.categories()) {
				rowIndex = writeTitleRow(sheet, styles, rowIndex, "[" + category.majorCategoryName() + "]");
				for (ExecutiveMemberEntries member : category.members()) {
					rowIndex = writeSubTitleRow(sheet, styles, rowIndex, member.name());
					rowIndex = writeEntrySection(sheet, styles, rowIndex, "금주 진행사항", member.thisWeekEntries());
					rowIndex++;
					rowIndex = writeEntrySection(sheet, styles, rowIndex, "차주 진행사항", member.nextWeekEntries());
					rowIndex++;
				}
				rowIndex++;
			}
			applyColumnWidths(sheet);
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("대표 뷰 xlsx 생성에 실패했습니다.", e);
		}
	}

	// ----- 내부 구현 -----

	private static int writeTitleRow(Sheet sheet, Styles styles, int rowIndex, String title) {
		Row row = sheet.createRow(rowIndex);
		Cell cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(styles.title);
		return rowIndex + 1;
	}

	private static int writeSubTitleRow(Sheet sheet, Styles styles, int rowIndex, String title) {
		Row row = sheet.createRow(rowIndex);
		Cell cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(styles.subTitle);
		return rowIndex + 1;
	}

	private static int writeEntrySection(Sheet sheet, Styles styles, int rowIndex, String sectionTitle,
			List<EntryResponse> entries) {
		rowIndex = writeSubTitleRow(sheet, styles, rowIndex, sectionTitle + " (" + entries.size() + "건)");

		Row headerRow = sheet.createRow(rowIndex++);
		for (int i = 0; i < COLUMNS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(COLUMNS[i]);
			cell.setCellStyle(styles.header);
		}

		if (entries.isEmpty()) {
			Row emptyRow = sheet.createRow(rowIndex++);
			Cell cell = emptyRow.createCell(0);
			cell.setCellValue("해당 없음");
			cell.setCellStyle(styles.body);
		} else {
			for (EntryResponse entry : entries) {
				Row row = sheet.createRow(rowIndex++);
				setCell(row, 0, entry.majorCategoryName(), styles.body);
				setCell(row, 1, entry.middleCategoryName(), styles.body);
				setCell(row, 2, entry.minorCategory(), styles.body);
				setCell(row, 3, entry.detail(), styles.body);
				Cell rateCell = row.createCell(4);
				rateCell.setCellValue(entry.ratePercent());
				rateCell.setCellStyle(styles.body);
			}
		}
		return rowIndex;
	}

	private static void setCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value != null ? value : "");
		cell.setCellStyle(style);
	}

	private static void applyColumnWidths(Sheet sheet) {
		for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i]);
		}
	}

	private static byte[] toBytes(XSSFWorkbook workbook) throws IOException {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			workbook.write(out);
			return out.toByteArray();
		}
	}

	/** 워크북 하나 안에서 재사용하는 셀 스타일 묶음(POI는 워크북마다 스타일 객체를 새로 만들어야 한다). */
	private static final class Styles {
		private final CellStyle title;
		private final CellStyle subTitle;
		private final CellStyle header;
		private final CellStyle body;

		private Styles(XSSFWorkbook workbook) {
			Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 13);
			title = workbook.createCellStyle();
			title.setFont(titleFont);

			Font subTitleFont = workbook.createFont();
			subTitleFont.setBold(true);
			subTitle = workbook.createCellStyle();
			subTitle.setFont(subTitleFont);

			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			header = workbook.createCellStyle();
			header.setFont(headerFont);
			header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			header.setAlignment(HorizontalAlignment.CENTER);
			header.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			header.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			header.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			header.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

			body = workbook.createCellStyle();
			body.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			body.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			body.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			body.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
			body.setWrapText(true);
		}
	}
}
