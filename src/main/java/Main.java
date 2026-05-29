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

        System.out.println("Запуск графического окна...");

        final InvertedIndex finalEngineForGui = engine;

        javax.swing.SwingUtilities.invokeLater(() -> {
            SearchEngineGUI gui = new SearchEngineGUI(finalEngineForGui);
            gui.setVisible(true);
        });
    }
}