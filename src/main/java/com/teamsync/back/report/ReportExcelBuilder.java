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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * FR-409(xlsx 내보내기)를 Apache POI로 렌더링하는 순수 빌더. PDF와 동일하게 "요청 시 즉석 렌더링,
 * 저장 안 함" 원칙을 따른다(ReportExcelExportService가 호출, byte[]를 즉시 반환).
 *
 * <p>개인/팀 보고서는 금주·차주 항목을 (대/중/소분류) 키로 매칭해 한 행에 좌우로 배치한다(같은 업무의
 * 이번 주 진행 상황과 다음 주 계획을 나란히 비교할 수 있도록). 대표 뷰는 대분류(프로젝트) 단위로 병합된
 * 표 형태로 렌더링한다.
 */
final class ReportExcelBuilder {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final String[] ENTRY_COLUMNS = {
			"대분류", "중분류", "소분류", "금주 상세업무", "금주 달성률(%)", "차주 상세업무", "차주 달성률(%)"
	};
	private static final int[] ENTRY_COLUMN_WIDTHS = {4200, 4200, 4200, 9500, 2800, 9500, 2800};

	private static final String[] EXECUTIVE_COLUMNS = {
			"프로젝트명", "프로젝트 달성률", "참여 인원", "담당자", "분류", "상세 업무 내용", "상태/달성률", "구분"
	};
	private static final int[] EXECUTIVE_COLUMN_WIDTHS = {6500, 3200, 5500, 3200, 3800, 11000, 4200, 2400};

	private ReportExcelBuilder() {
	}

	static byte[] buildIndividualWorkbook(WeeklyReportExportView view) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			WeeklyReportResponse r = view.report();
			Sheet sheet = workbook.createSheet("주간보고");
			Styles styles = new Styles(workbook);
			int rowIndex = writeTitleRow(sheet, styles, 0,
					view.authorName() + " 주간 보고 (" + r.weekStart().format(DATE) + " ~ " + r.weekEnd().format(DATE) + ")",
					ENTRY_COLUMNS.length);
			rowIndex++;
			writeEntrySection(sheet, styles, rowIndex, r.entries().thisWeek(), r.entries().nextWeek());
			applyColumnWidths(sheet, ENTRY_COLUMN_WIDTHS);
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
							+ r.submittedCount() + "/" + r.totalMemberCount(),
					ENTRY_COLUMNS.length);
			rowIndex++;

			for (TeamMemberReportEntries member : r.members()) {
				rowIndex = writeSubTitleRow(sheet, styles, rowIndex, member.name() + " ("
						+ (member.status() == MemberSubmissionStatus.SUBMITTED ? "제출 완료" : "미제출") + ")",
						ENTRY_COLUMNS.length);
				rowIndex = writeEntrySection(sheet, styles, rowIndex, member.thisWeekEntries(), member.nextWeekEntries());
				rowIndex++;
			}
			applyColumnWidths(sheet, ENTRY_COLUMN_WIDTHS);
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("팀 주간 보고 xlsx 생성에 실패했습니다.", e);
		}
	}

	/**
	 * 대표 뷰: 대분류(프로젝트)별로 프로젝트명/달성률/참여 인원을 병합한 한 표로 렌더링한다. 같은
	 * 업무라도 금주/차주 계획이 각각 다른 행으로 나오되 "구분" 칼럼과 배경색으로 구별한다.
	 */
	static byte[] buildExecutiveWorkbook(ExecutiveDashboardResponse view) {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("대표 뷰");
			Styles styles = new Styles(workbook);
			int rowIndex = writeTitleRow(sheet, styles, 0,
					"대표 뷰 (" + view.weekStart().format(DATE) + " ~ " + view.weekEnd().format(DATE) + ")",
					EXECUTIVE_COLUMNS.length);
			rowIndex++;

			int headerRowIndex = rowIndex;
			writeExecutiveHeaderRow(sheet, styles, headerRowIndex);
			rowIndex = headerRowIndex + 1;
			sheet.createFreezePane(0, rowIndex);

			for (ExecutiveCategoryGroup category : view.categories()) {
				rowIndex = writeExecutiveCategoryBlock(sheet, styles, rowIndex, category);
			}

			if (rowIndex > headerRowIndex + 1) {
				sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, headerRowIndex,
						0, EXECUTIVE_COLUMNS.length - 1));
			}
			applyColumnWidths(sheet, EXECUTIVE_COLUMN_WIDTHS);
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("대표 뷰 xlsx 생성에 실패했습니다.", e);
		}
	}

	// ----- 대표 뷰 -----

	private static void writeExecutiveHeaderRow(Sheet sheet, Styles styles, int rowIndex) {
		Row headerRow = sheet.createRow(rowIndex);
		for (int i = 0; i < EXECUTIVE_COLUMNS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(EXECUTIVE_COLUMNS[i]);
			cell.setCellStyle(styles.header);
		}
	}

	private static int writeExecutiveCategoryBlock(Sheet sheet, Styles styles, int rowIndex,
			ExecutiveCategoryGroup category) {
		record ExecutiveRow(String memberName, EntryResponse entry, String weekLabel) {
		}

		List<ExecutiveRow> rows = new ArrayList<>();
		Set<String> participants = new LinkedHashSet<>();
		int rateSum = 0;

		for (ExecutiveMemberEntries member : category.members()) {
			if (!member.thisWeekEntries().isEmpty() || !member.nextWeekEntries().isEmpty()) {
				participants.add(member.name());
			}
			for (EntryResponse entry : member.thisWeekEntries()) {
				rows.add(new ExecutiveRow(member.name(), entry, "금주"));
				rateSum += entry.ratePercent();
			}
			for (EntryResponse entry : member.nextWeekEntries()) {
				rows.add(new ExecutiveRow(member.name(), entry, "차주"));
				rateSum += entry.ratePercent();
			}
		}

		if (rows.isEmpty()) {
			return rowIndex;
		}

		int blockStart = rowIndex;
		int projectRate = Math.round((float) rateSum / rows.size());
		String participantsLabel = String.join(", ", participants);

		boolean zebra = false;
		for (ExecutiveRow entryRow : rows) {
			Row row = sheet.createRow(rowIndex++);
			CellStyle rowStyle = zebra ? styles.bodyAlt : styles.body;
			zebra = !zebra;

			setCell(row, 0, category.majorCategoryName(), styles.projectMerged);
			setNumericCell(row, 1, projectRate, styles.projectMergedCenter);
			setCell(row, 2, participantsLabel, styles.projectMerged);
			setCell(row, 3, entryRow.memberName(), rowStyle);
			setCell(row, 4, entryRow.entry().middleCategoryName(), rowStyle);
			setCell(row, 5, entryRow.entry().detail(), rowStyle);
			setCell(row, 6, statusLabel(entryRow.entry().ratePercent()), rowStyle);
			Cell weekCell = row.createCell(7);
			weekCell.setCellValue(entryRow.weekLabel());
			weekCell.setCellStyle("금주".equals(entryRow.weekLabel()) ? styles.weekThisBadge : styles.weekNextBadge);
		}

		int blockEnd = rowIndex - 1;
		if (blockEnd > blockStart) {
			mergeIfMultiRow(sheet, blockStart, blockEnd, 0);
			mergeIfMultiRow(sheet, blockStart, blockEnd, 1);
			mergeIfMultiRow(sheet, blockStart, blockEnd, 2);
		}
		return rowIndex;
	}

	private static String statusLabel(int ratePercent) {
		if (ratePercent >= 100) {
			return "완료 (100%)";
		}
		if (ratePercent <= 0) {
			return "예정 (0%)";
		}
		return "진행 중 (" + ratePercent + "%)";
	}

	private static void mergeIfMultiRow(Sheet sheet, int firstRow, int lastRow, int column) {
		sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, column, column));
	}

	// ----- 개인/팀: 금주·차주 매칭 표 -----

	/** 금주/차주 항목을 (대/중/소분류) 키로 매칭해 한 행에 좌우로 나란히 배치한다. */
	private static int writeEntrySection(Sheet sheet, Styles styles, int rowIndex,
			List<EntryResponse> thisWeek, List<EntryResponse> nextWeek) {
		Row headerRow = sheet.createRow(rowIndex++);
		for (int i = 0; i < ENTRY_COLUMNS.length; i++) {
			Cell cell = headerRow.createCell(i);
			cell.setCellValue(ENTRY_COLUMNS[i]);
			cell.setCellStyle(i < 3 ? styles.header : (i < 5 ? styles.headerThisWeek : styles.headerNextWeek));
		}

		List<MatchedEntryRow> matched = matchEntries(thisWeek, nextWeek);
		if (matched.isEmpty()) {
			Row emptyRow = sheet.createRow(rowIndex++);
			Cell cell = emptyRow.createCell(0);
			cell.setCellValue("해당 없음");
			cell.setCellStyle(styles.body);
			sheet.addMergedRegion(new CellRangeAddress(emptyRow.getRowNum(), emptyRow.getRowNum(),
					0, ENTRY_COLUMNS.length - 1));
			return rowIndex;
		}

		boolean zebra = false;
		for (MatchedEntryRow pair : matched) {
			Row row = sheet.createRow(rowIndex++);
			CellStyle rowStyle = zebra ? styles.bodyAlt : styles.body;
			zebra = !zebra;

			EntryResponse category = pair.thisWeek() != null ? pair.thisWeek() : pair.nextWeek();
			setCell(row, 0, category.majorCategoryName(), rowStyle);
			setCell(row, 1, category.middleCategoryName(), rowStyle);
			setCell(row, 2, category.minorCategory(), rowStyle);

			if (pair.thisWeek() != null) {
				setCell(row, 3, pair.thisWeek().detail(), rowStyle);
				setNumericCell(row, 4, pair.thisWeek().ratePercent(), rowStyle);
			} else {
				setCell(row, 3, "-", rowStyle);
				setCell(row, 4, "-", rowStyle);
			}

			if (pair.nextWeek() != null) {
				setCell(row, 5, pair.nextWeek().detail(), rowStyle);
				setNumericCell(row, 6, pair.nextWeek().ratePercent(), rowStyle);
			} else {
				setCell(row, 5, "-", rowStyle);
				setCell(row, 6, "-", rowStyle);
			}
		}
		return rowIndex;
	}

	private record MatchedEntryRow(EntryResponse thisWeek, EntryResponse nextWeek) {
	}

	private static List<MatchedEntryRow> matchEntries(List<EntryResponse> thisWeek, List<EntryResponse> nextWeek) {
		List<MatchedEntryRow> result = new ArrayList<>();
		List<EntryResponse> remainingNext = new ArrayList<>(nextWeek);

		for (EntryResponse tw : thisWeek) {
			EntryResponse match = extractMatch(remainingNext, tw);
			result.add(new MatchedEntryRow(tw, match));
		}
		for (EntryResponse nw : remainingNext) {
			result.add(new MatchedEntryRow(null, nw));
		}
		return result;
	}

	private static EntryResponse extractMatch(List<EntryResponse> pool, EntryResponse target) {
		Iterator<EntryResponse> it = pool.iterator();
		while (it.hasNext()) {
			EntryResponse candidate = it.next();
			if (sameCategory(candidate, target)) {
				it.remove();
				return candidate;
			}
		}
		return null;
	}

	private static boolean sameCategory(EntryResponse a, EntryResponse b) {
		return Objects.equals(a.majorCategoryId(), b.majorCategoryId())
				&& Objects.equals(a.middleCategoryId(), b.middleCategoryId())
				&& Objects.equals(a.minorCategory(), b.minorCategory());
	}

	// ----- 공통 -----

	private static int writeTitleRow(Sheet sheet, Styles styles, int rowIndex, String title, int columnSpan) {
		Row row = sheet.createRow(rowIndex);
		Cell cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(styles.title);
		for (int i = 1; i < columnSpan; i++) {
			Cell fillerCell = row.createCell(i);
			fillerCell.setCellStyle(styles.title);
		}
		if (columnSpan > 1) {
			sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnSpan - 1));
		}
		return rowIndex + 1;
	}

	private static int writeSubTitleRow(Sheet sheet, Styles styles, int rowIndex, String title, int columnSpan) {
		Row row = sheet.createRow(rowIndex);
		Cell cell = row.createCell(0);
		cell.setCellValue(title);
		cell.setCellStyle(styles.subTitle);
		for (int i = 1; i < columnSpan; i++) {
			Cell fillerCell = row.createCell(i);
			fillerCell.setCellStyle(styles.subTitle);
		}
		if (columnSpan > 1) {
			sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, columnSpan - 1));
		}
		return rowIndex + 1;
	}

	private static void setCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value != null ? value : "");
		cell.setCellStyle(style);
	}

	private static void setNumericCell(Row row, int column, int value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private static void applyColumnWidths(Sheet sheet, int[] widths) {
		for (int i = 0; i < widths.length; i++) {
			sheet.setColumnWidth(i, widths[i]);
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
		private final CellStyle headerThisWeek;
		private final CellStyle headerNextWeek;
		private final CellStyle body;
		private final CellStyle bodyAlt;
		private final CellStyle projectMerged;
		private final CellStyle projectMergedCenter;
		private final CellStyle weekThisBadge;
		private final CellStyle weekNextBadge;

		private Styles(XSSFWorkbook workbook) {
			Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 14);
			titleFont.setColor(IndexedColors.WHITE.getIndex());
			title = workbook.createCellStyle();
			title.setFont(titleFont);
			title.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
			title.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			title.setVerticalAlignment(VerticalAlignment.CENTER);

			Font subTitleFont = workbook.createFont();
			subTitleFont.setBold(true);
			subTitleFont.setFontHeightInPoints((short) 11);
			subTitle = workbook.createCellStyle();
			subTitle.setFont(subTitleFont);
			subTitle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			subTitle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			subTitle.setVerticalAlignment(VerticalAlignment.CENTER);

			header = baseHeaderStyle(workbook, IndexedColors.GREY_40_PERCENT.getIndex(), true);
			headerThisWeek = baseHeaderStyle(workbook, null, false);
			applyCustomFill((XSSFCellStyle) headerThisWeek, new XSSFColor(new byte[] {(byte) 0xBD, (byte) 0xD7, (byte) 0xEE}, null));
			headerNextWeek = baseHeaderStyle(workbook, null, false);
			applyCustomFill((XSSFCellStyle) headerNextWeek, new XSSFColor(new byte[] {(byte) 0xFC, (byte) 0xE4, (byte) 0xD6}, null));

			body = bodyStyle(workbook, false);
			bodyAlt = bodyStyle(workbook, true);

			projectMerged = workbook.createCellStyle();
			projectMerged.cloneStyleFrom(body);
			projectMerged.setAlignment(HorizontalAlignment.CENTER);
			projectMerged.setVerticalAlignment(VerticalAlignment.CENTER);
			Font projectFont = workbook.createFont();
			projectFont.setBold(true);
			projectMerged.setFont(projectFont);

			projectMergedCenter = workbook.createCellStyle();
			projectMergedCenter.cloneStyleFrom(projectMerged);
			org.apache.poi.ss.usermodel.DataFormat format = workbook.createDataFormat();
			projectMergedCenter.setDataFormat(format.getFormat("0\"%\""));

			weekThisBadge = workbook.createCellStyle();
			weekThisBadge.cloneStyleFrom(body);
			weekThisBadge.setAlignment(HorizontalAlignment.CENTER);
			applyCustomFill((XSSFCellStyle) weekThisBadge, new XSSFColor(new byte[] {(byte) 0xDD, (byte) 0xEB, (byte) 0xF7}, null));

			weekNextBadge = workbook.createCellStyle();
			weekNextBadge.cloneStyleFrom(body);
			weekNextBadge.setAlignment(HorizontalAlignment.CENTER);
			applyCustomFill((XSSFCellStyle) weekNextBadge, new XSSFColor(new byte[] {(byte) 0xFD, (byte) 0xEA, (byte) 0xD7}, null));
		}

		private static CellStyle baseHeaderStyle(XSSFWorkbook workbook, Short indexedColor, boolean whiteFont) {
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			if (whiteFont) {
				headerFont.setColor(IndexedColors.WHITE.getIndex());
			}
			CellStyle style = workbook.createCellStyle();
			style.setFont(headerFont);
			if (indexedColor != null) {
				style.setFillForegroundColor(indexedColor);
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			}
			style.setAlignment(HorizontalAlignment.CENTER);
			style.setVerticalAlignment(VerticalAlignment.CENTER);
			applyThinBorder(style);
			return style;
		}

		private static void applyCustomFill(XSSFCellStyle style, XSSFColor color) {
			style.setFillForegroundColor(color);
			style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}

		private static CellStyle bodyStyle(XSSFWorkbook workbook, boolean zebra) {
			CellStyle style = workbook.createCellStyle();
			style.setWrapText(true);
			style.setVerticalAlignment(VerticalAlignment.CENTER);
			applyThinBorder(style);
			if (zebra) {
				((XSSFCellStyle) style).setFillForegroundColor(
						new XSSFColor(new byte[] {(byte) 0xF5, (byte) 0xF5, (byte) 0xF5}, null));
				style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			}
			return style;
		}

		private static void applyThinBorder(CellStyle style) {
			style.setBorderBottom(BorderStyle.THIN);
			style.setBorderTop(BorderStyle.THIN);
			style.setBorderLeft(BorderStyle.THIN);
			style.setBorderRight(BorderStyle.THIN);
		}
	}
}
