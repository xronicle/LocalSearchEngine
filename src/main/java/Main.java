import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Path folderPath = Paths.get("test_documents");

        InvertedIndex engine = new InvertedIndex();

        System.out.println("Индексируем файлы в папке " + folderPath + "...");

        try (Stream<Path> paths = Files.list(folderPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".txt") || name.endsWith(".pdf") || name.endsWith(".docx");
                    })
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        try {
                            String content = FileParser.extractText(filePath);

                            content = content.toLowerCase().replaceAll("[^a-zа-яё0-9\\s]", "");
                            String[] words = content.split("\\s+");

                            for (String word : words) {
                                engine.addWord(word, fileName);
                            }
                        } catch (Exception e) {
                            System.out.println("Ошибка чтения файла: " + fileName);
                        }
                    });

            System.out.println("Индексация завершена. Уникальных слов: " + engine.getSize());
            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.print("\nВведите запрос (или 'выход' для завершения): ");
                String query = scanner.nextLine().toLowerCase().trim();

                if (query.equals("выход")) {
                    System.out.println("Поиск завершен.");
                    break;
                }

                String[] queryWords = query.split("\\s+");

                Map<String, Double> combinedResults = new HashMap<>();

                for (String qWord : queryWords) {
                    if (qWord.isEmpty()) continue;

                    Map<String, Double> wordResults = engine.searchTfIdf(qWord);

                    for (Map.Entry<String, Double> entry : wordResults.entrySet()) {
                        String docName = entry.getKey();
                        double score = entry.getValue();

                        combinedResults.put(docName, combinedResults.getOrDefault(docName, 0.0) + score);
                    }
                }

                if (!combinedResults.isEmpty()) {
                    System.out.println("Результаты поиска (по суммарной TF-IDF релевантности):");

                    combinedResults.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .forEach(entry -> {
                                System.out.printf("%s (Суммарный вес: %.4f)\n", entry.getKey(), entry.getValue());
                            });
                } else {
                    System.out.println("По вашему запросу ничего не найдено.");
                }
            }

        } catch (Exception e) {
            System.out.println("Критическая ошибка: невозможно получить доступ к папке " + folderPath);
            e.printStackTrace();
        }
    }
}