package com.teamsync.back.report.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * FR-409(보고서 내보내기): 완성된 XHTML 문자열을 PDF 바이트로 렌더링하는 얇은 래퍼
 * (openhtmltopdf-pdfbox). PDFBox 내장 Base14 폰트는 한글 글리프가 없어, 배포 서버(리눅스 컨테이너
 * 등)에 한글 폰트가 설치되어 있지 않으면 PDF에 빈 사각형(tofu)만 남는다. 이를 피하기 위해 OFL
 * 라이선스 한글 폰트(NanumGothic-Regular.ttf, resources/fonts)를 애플리케이션에 번들해 항상
 * 임베드한다 — HTML 쪽에서 font-family: 'NanumGothic'을 지정해야 실제로 적용된다
 * (ReportExportHtmlBuilder의 공통 CSS 참고).
 */
@Component
public class ReportPdfRenderer {

	public static final String FONT_FAMILY = "NanumGothic";
	private static final String FONT_RESOURCE = "fonts/NanumGothic-Regular.ttf";

	public byte[] render(String xhtml) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(xhtml, null);
			// FSSupplier는 실제 PDF 빌드(run()) 시점에 지연 호출되므로, 매 호출마다 새 스트림을
			// 여는 람다로 등록해야 한다(스트림을 미리 열어 재사용하면 두 번째 렌더링부터 깨진다).
			builder.useFont(this::openFontStream, FONT_FAMILY);
			builder.toStream(out);
			builder.run();
			return out.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("PDF 렌더링에 실패했습니다.", e);
		}
	}

	private InputStream openFontStream() {
		try {
			return new ClassPathResource(FONT_RESOURCE).getInputStream();
		} catch (IOException e) {
			throw new IllegalStateException("PDF 폰트 리소스를 열 수 없습니다: " + FONT_RESOURCE, e);
		}
	}
}
