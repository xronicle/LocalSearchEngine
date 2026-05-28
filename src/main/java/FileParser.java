import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileParser {
    
    public static String extractText(Path filePath) throws Exception {
        String fileName = filePath.getFileName().toString().toLowerCase();

        if (fileName.endsWith(".txt")) {
            return Files.readString(filePath);
        }
        else if (fileName.endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(filePath.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        else if (fileName.endsWith(".docx")) {
            try (FileInputStream fis = new FileInputStream(filePath.toFile());
                 XWPFDocument document = new XWPFDocument(fis);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }

        return "";
    }
}