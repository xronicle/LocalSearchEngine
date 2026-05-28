import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                    .filter(p -> p.toString().endsWith(".txt"))
                    .forEach(filePath -> {
                        String fileName = filePath.getFileName().toString();
                        try {
                            String content = Files.readString(filePath);
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
                System.out.print("\nВведите слово для поиска (или 'выход' для завершения): ");
                String query = scanner.nextLine().toLowerCase().trim();

                if (query.equals("выход")) {
                    System.out.println("Поиск завершен.");
                    break;
                }

                Map<String, Integer> foundIn = engine.search(query);

                if (!foundIn.isEmpty()) {
                    System.out.println("Результаты поиска (по релевантности):");

                    foundIn.entrySet().stream()
                            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                            .forEach(entry -> {
                                System.out.println(entry.getKey() + " (совпадений: " + entry.getValue() + ")");
                            });
                } else {
                    System.out.println("Слово не найдено.");
                }
            }

        } catch (Exception e) {
            System.out.println("Критическая ошибка: невозможно получить доступ к папке " + folderPath);
            e.printStackTrace();
        }
    }
}