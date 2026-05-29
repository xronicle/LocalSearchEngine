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
        Path indexPath = Paths.get("index.json");

        InvertedIndex engine = null;

        if (Files.exists(indexPath)) {
            System.out.println("Найден сохраненный индекс! Быстрая загрузка из базы...");
            try {
                engine = InvertedIndex.loadFromFile(indexPath.toString());
                System.out.println("Загрузка завершена. Уникальных корней в базе: " + engine.getSize());
            } catch (Exception e) {
                System.out.println("Ошибка при чтении базы. Индекс будет пересобран.");
            }
        }

        if (engine == null) {
            engine = new InvertedIndex();
            System.out.println("База не найдена. Начинаем глубокое индексирование папки " + folderPath + "...");

            try (Stream<Path> paths = Files.list(folderPath)) {
                InvertedIndex finalEngine = engine;
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
                                    if (word.isEmpty()) continue;
                                    String stemmedWord = RussianStemmer.stem(word);
                                    finalEngine.addWord(stemmedWord, fileName);
                                }
                            } catch (Exception e) {
                                System.out.println("Ошибка чтения файла: " + fileName);
                            }
                        });

                System.out.println("Индексация завершена. Уникальных корней: " + engine.getSize());

                System.out.println("Сохраняем индекс на диск...");
                engine.saveToFile(indexPath.toString());
                System.out.println("База успешно сохранена в файл index.json");

            } catch (Exception e) {
                System.out.println("Критическая ошибка: невозможно получить доступ к папке " + folderPath);
                e.printStackTrace();
            }
        }

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

                String stemmedQuery = RussianStemmer.stem(qWord);

                Map<String, Double> wordResults = engine.searchTfIdf(stemmedQuery);

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
                            System.out.printf(" %s (Суммарный вес: %.4f)\n", entry.getKey(), entry.getValue());
                        });
            } else {
                System.out.println("По вашему запросу ничего не найдено.");
            }
        }
    }
}