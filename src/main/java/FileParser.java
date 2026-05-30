import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.sl.extractor.SlideShowExtractor;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileParser {

    public static String extractText(Path path) throws Exception {
        String name = path.toString().toLowerCase();

        if (name.endsWith(".txt")) {
            return new String(Files.readAllBytes(path), "UTF-8");
        }

        if (name.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(path.toFile())) {
                return new PDFTextStripper().getText(document);
            }
        }

        if (name.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 XWPFDocument document = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }

        if (name.endsWith(".xlsx")) {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 XSSFWorkbook workbook = new XSSFWorkbook(fis);
                 XSSFExcelExtractor extractor = new XSSFExcelExtractor(workbook)) {
                return extractor.getText();
            }
        }

        if (name.endsWith(".pptx")) {
            try (FileInputStream fis = new FileInputStream(path.toFile());
                 XMLSlideShow slideshow = new XMLSlideShow(fis);
                 SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideshow)) {
                return extractor.getText();
            }
        }

        return "";
    }
}