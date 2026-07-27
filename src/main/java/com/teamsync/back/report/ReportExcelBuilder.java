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

	private static final int EXECUTIVE_FIXED_COLUMNS = 3; // No. / 프로젝트명 / 담당자
	private static final int EXECUTIVE_MONTH_COUNT = 12;
	private static final int EXECUTIVE_WEEKS_PER_MONTH = 4;
	private static final int[] EXECUTIVE_FIXED_COLUMN_WIDTHS = {1800, 7500, 5500};
	private static final int EXECUTIVE_WEEK_COLUMN_WIDTH = 1800;

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
	 * 프로젝트 진척률: 대분류(프로젝트) 1개당 한 행으로, 연간 12개월×4주 그리드에서 조회한 주에 해당하는
	 * 칸에만 달성률을 표시한다(이월 데이터가 없어 다른 주/월 칸은 비워 둔 채로 매주 갱신하며 채워 나간다).
	 */
	static byte[] buildExecutiveWorkbook(ExecutiveDashboardResponse view) {
		int totalColumns = EXECUTIVE_FIXED_COLUMNS + EXECUTIVE_MONTH_COUNT * EXECUTIVE_WEEKS_PER_MONTH;
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("프로젝트 진척률");
			Styles styles = new Styles(workbook);
			int rowIndex = writeTitleRow(sheet, styles, 0, "프로젝트 진척률(%)", totalColumns);

			int headerTopRow = rowIndex;
			int headerBottomRow = headerTopRow + 2;
			writeExecutiveHeaderRows(sheet, styles, headerTopRow, view.weekStart().getYear());
			rowIndex = headerBottomRow + 1;
			sheet.createFreezePane(EXECUTIVE_FIXED_COLUMNS, rowIndex);

			int monthIndex = view.weekStart().getMonthValue() - 1;
			int weekIndex = weekIndexOf(view.weekStart());

			int no = 1;
			for (ExecutiveCategoryGroup category : view.categories()) {
				Integer rate = projectRate(category);
				if (rate == null) {
					continue;
				}
				writeExecutiveProjectRow(sheet, styles, rowIndex++, no++, category, rate, monthIndex, weekIndex);
			}

			if (rowIndex > headerBottomRow + 1) {
				sheet.setAutoFilter(new CellRangeAddress(headerBottomRow, headerBottomRow, 0, totalColumns - 1));
			}
			applyColumnWidths(sheet, executiveColumnWidths());
			return toBytes(workbook);
		} catch (IOException e) {
			throw new UncheckedIOException("프로젝트 진척률 xlsx 생성에 실패했습니다.", e);
		}
	}

	// ----- 프로젝트 진척률 -----

	private static int weekIndexOf(java.time.LocalDate date) {
		int day = date.getDayOfMonth();
		if (day <= 7) {
			return 0;
		}
		if (day <= 14) {
			return 1;
		}
		if (day <= 21) {
			return 2;
		}
		return 3;
	}

	private static int[] executiveColumnWidths() {
		int[] widths = new int[EXECUTIVE_FIXED_COLUMNS + EXECUTIVE_MONTH_COUNT * EXECUTIVE_WEEKS_PER_MONTH];
		System.arraycopy(EXECUTIVE_FIXED_COLUMN_WIDTHS, 0, widths, 0, EXECUTIVE_FIXED_COLUMNS);
		for (int i = EXECUTIVE_FIXED_COLUMNS; i < widths.length; i++) {
			widths[i] = EXECUTIVE_WEEK_COLUMN_WIDTH;
		}
		return widths;
	}

	/** No./프로젝트명/담당자(3행 세로 병합) + 연도/월/주 3단 그리드 헤더를 만든다. */
	private static void writeExecutiveHeaderRows(Sheet sheet, Styles styles, int topRow, int year) {
		Row yearRow = sheet.createRow(topRow);
		Row monthRow = sheet.createRow(topRow + 1);
		Row weekRow = sheet.createRow(topRow + 2);

		String[] fixedLabels = {"No.", "프로젝트명", "담당자"};
		for (int i = 0; i < fixedLabels.length; i++) {
			setCell(yearRow, i, fixedLabels[i], styles.header);
			monthRow.createCell(i).setCellStyle(styles.header);
			weekRow.createCell(i).setCellStyle(styles.header);
			mergeIfMultiRow(sheet, topRow, topRow + 2, i);
		}

		int gridStart = EXECUTIVE_FIXED_COLUMNS;
		int gridEnd = gridStart + EXECUTIVE_MONTH_COUNT * EXECUTIVE_WEEKS_PER_MONTH - 1;
		setCell(yearRow, gridStart, year + "년", styles.header);
		for (int c = gridStart + 1; c <= gridEnd; c++) {
			yearRow.createCell(c).setCellStyle(styles.header);
		}
		sheet.addMergedRegion(new CellRangeAddress(topRow, topRow, gridStart, gridEnd));

		for (int m = 0; m < EXECUTIVE_MONTH_COUNT; m++) {
			int monthStart = gridStart + m * EXECUTIVE_WEEKS_PER_MONTH;
			int monthEnd = monthStart + EXECUTIVE_WEEKS_PER_MONTH - 1;
			setCell(monthRow, monthStart, (m + 1) + "월", styles.header);
			for (int c = monthStart + 1; c <= monthEnd; c++) {
				monthRow.createCell(c).setCellStyle(styles.header);
			}
			sheet.addMergedRegion(new CellRangeAddress(topRow + 1, topRow + 1, monthStart, monthEnd));

			for (int w = 0; w < EXECUTIVE_WEEKS_PER_MONTH; w++) {
				setCell(weekRow, monthStart + w, "W" + (w + 1), styles.header);
			}
		}
	}

	private static Integer projectRate(ExecutiveCategoryGroup category) {
		int rateSum = 0;
		int count = 0;
		for (ExecutiveMemberEntries member : category.members()) {
			for (EntryResponse entry : member.thisWeekEntries()) {
				rateSum += entry.ratePercent();
				count++;
			}
			for (EntryResponse entry : member.nextWeekEntries()) {
				rateSum += entry.ratePercent();
				count++;
			}
		}
		return count == 0 ? null : Math.round((float) rateSum / count);
	}

	private static void writeExecutiveProjectRow(Sheet sheet, Styles styles, int rowIndex, int no,
			ExecutiveCategoryGroup category, int projectRate, int monthIndex, int weekIndex) {
		Set<String> participants = new LinkedHashSet<>();
		for (ExecutiveMemberEntries member : category.members()) {
			if (!member.thisWeekEntries().isEmpty() || !member.nextWeekEntries().isEmpty()) {
				participants.add(member.name());
			}
		}
		String participantsLabel = String.join(", ", participants);

		Row row = sheet.createRow(rowIndex);
		setNumericCell(row, 0, no, styles.projectMerged);
		setCell(row, 1, category.majorCategoryName(), styles.projectMerged);
		setCell(row, 2, participantsLabel, styles.projectMerged);

		int gridStart = EXECUTIVE_FIXED_COLUMNS;
		int gridEnd = gridStart + EXECUTIVE_MONTH_COUNT * EXECUTIVE_WEEKS_PER_MONTH - 1;
		for (int c = gridStart; c <= gridEnd; c++) {
			row.createCell(c).setCellStyle(styles.body);
		}

		int rateColumn = gridStart + monthIndex * EXECUTIVE_WEEKS_PER_MONTH + weekIndex;
		Cell rateCell = row.getCell(rateColumn);
		rateCell.setCellValue(projectRate + "%");
		rateCell.setCellStyle(styles.weekThisBadge);
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
		private final CellStyle weekThisBadge;

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

			weekThisBadge = workbook.createCellStyle();
			weekThisBadge.cloneStyleFrom(body);
			weekThisBadge.setAlignment(HorizontalAlignment.CENTER);
			applyCustomFill((XSSFCellStyle) weekThisBadge, new XSSFColor(new byte[] {(byte) 0xDD, (byte) 0xEB, (byte) 0xF7}, null));
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
