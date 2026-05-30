import java.nio.file.Paths;
import java.util.regex.Pattern;

public class SnippetGenerator {

    public static String generateSnippet(String filePath, String stemmedQuery, int totalMentions) {
        try {
            String content = FileParser.extractText(Paths.get(filePath));

            content = content.replaceAll("[\\r\\n\\t]+", " ");

            String lowerContent = content.toLowerCase();
            int index = lowerContent.indexOf(stemmedQuery);

            if (index == -1) {
                return "<html><i style='color:gray'>Совпадение скрыто в метаданных</i></html>";
            }

            int start = Math.max(0, index - 40);
            int end = Math.min(content.length(), index + stemmedQuery.length() + 40);

            String snippet = content.substring(start, end);

            String regex = "(?iu)(" + Pattern.quote(stemmedQuery) + "[а-яёa-z]*)";
            snippet = snippet.replaceAll(regex, "<b style='color:#4A90E2'>$1</b>");

            String prefix = (start > 0) ? "..." : "";
            String suffix = (end < content.length()) ? "..." : "";

            StringBuilder html = new StringBuilder();
            html.append("<html><body style='width: 350px; margin-top: 4px; margin-bottom: 4px;'>");
            html.append(prefix).append(snippet).append(suffix);

            if (totalMentions > 1) {
                html.append("<br><span style='color: gray; font-size: 85%;'>[+ еще ")
                        .append(totalMentions - 1).append(" упоминаний в тексте]</span>");
            }
            html.append("</body></html>");

            return html.toString();
        } catch (Exception e) {
            return "<html><i style='color:red'>Ошибка генерации контекста</i></html>";
        }
    }
}