package com.teamsync.back.report;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * FR-409: openhtmltopdf 렌더링 스모크 테스트. 한글이 포함된 XHTML을 넣어도 예외 없이 PDF 바이트가
 * 만들어지는지(번들 한글 폰트 로딩 포함)만 확인한다 — 실제 렌더 결과 픽셀 검증은 이번 스코프 밖.
 */
class ReportPdfRendererTest {

	private final ReportPdfRenderer renderer = new ReportPdfRenderer();

	@Test
	void 한글이_포함된_HTML을_PDF_바이트로_렌더링한다() {
		String html = "<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
				+ "<meta charset=\"UTF-8\" /><style>body{font-family:'" + ReportPdfRenderer.FONT_FAMILY
				+ "', sans-serif;}</style></head><body><h1>주간 보고서</h1><p>한글 렌더링 테스트</p></body></html>";

		byte[] pdf = renderer.render(html);

		assertThat(pdf).isNotEmpty();
		assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
	}
}
