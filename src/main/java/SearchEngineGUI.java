import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SearchEngineGUI extends JFrame {
    private JTextField searchField;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> sortBox;
    private InvertedIndex engine;
    private final String INDEX_FILE = "index.json";

    public SearchEngineGUI() {
        engine = new InvertedIndex();
        loadDatabaseIfExists();

        setTitle("Локальная Поисковая Система");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton searchButton = new JButton("Найти");
        JButton chooseFolderButton = new JButton("Выбрать папку");
        buttonPanel.add(searchButton);
        buttonPanel.add(chooseFolderButton);

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sortPanel.add(new JLabel("Сортировка: "));
        sortBox = new JComboBox<>(new String[]{"По релевантности (TF-IDF)", "По имени файла (А-Я)"});
        sortPanel.add(sortBox);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(searchField, BorderLayout.CENTER);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        headerPanel.add(sortPanel, BorderLayout.SOUTH);

        topPanel.add(headerPanel, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(new String[]{"Документ", "Вес (TF-IDF)", "Скрытый путь"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        resultTable.setRowHeight(25);
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));

        resultTable.getColumnModel().getColumn(2).setMinWidth(0);
        resultTable.getColumnModel().getColumn(2).setMaxWidth(0);
        resultTable.getColumnModel().getColumn(2).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(resultTable);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        sortBox.addActionListener(e -> performSearch()); // Пересортировка при смене
        chooseFolderButton.addActionListener(e -> selectFolderAndIndex());

        resultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2 && resultTable.getSelectedRow() != -1) {
                    openSelectedFile();
                }
            }
        });
    }

    private void loadDatabaseIfExists() {
        if (Files.exists(Paths.get(INDEX_FILE))) {
            try {
                engine = InvertedIndex.loadFromFile(INDEX_FILE);
            } catch (Exception ignored) {}
        }
    }

    private void selectFolderAndIndex() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите папку для индексации");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"Индексация...", "Пожалуйста, подождите", ""});

            new Thread(() -> {
                engine = new InvertedIndex();
                try (Stream<Path> paths = Files.list(selectedFolder.toPath())) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> {
                                String name = p.toString().toLowerCase();
                                return !name.startsWith("~$") && (name.endsWith(".txt") || name.endsWith(".pdf") || name.endsWith(".docx"));
                            })
                            .forEach(filePath -> {
                                try {
                                    String content = FileParser.extractText(filePath);
                                    content = content.toLowerCase().replaceAll("[^a-zа-яё0-9\\s]", "");
                                    String[] words = content.split("\\s+");

                                    // ВАЖНО: Теперь сохраняем АБСОЛЮТНЫЙ путь к файлу
                                    String absolutePath = filePath.toAbsolutePath().toString();
                                    for (String word : words) {
                                        if (word.isEmpty()) continue;
                                        engine.addWord(RussianStemmer.stem(word), absolutePath);
                                    }
                                } catch (Exception ex) {
                                    System.out.println("Пропущен файл: " + filePath.getFileName());
                                }
                            });

                    engine.saveToFile(INDEX_FILE);

                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        JOptionPane.showMessageDialog(this, "Индексация завершена!\nУникальных корней: " + engine.getSize());
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Ошибка доступа к папке."));
                }
            }).start();
        }
    }

    private void performSearch() {
        if (engine.getSize() == 0) return;
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) return;

        String[] queryWords = query.split("\\s+");
        Map<String, Double> combinedResults = new java.util.HashMap<>();

        for (String qWord : queryWords) {
            if (qWord.isEmpty()) continue;
            String stemmedQuery = RussianStemmer.stem(qWord);
            Map<String, Double> wordResults = engine.searchTfIdf(stemmedQuery);

            for (Map.Entry<String, Double> entry : wordResults.entrySet()) {
                String fullPath = entry.getKey();
                double score = entry.getValue();
                combinedResults.put(fullPath, combinedResults.getOrDefault(fullPath, 0.0) + score);
            }
        }


        tableModel.setRowCount(0);

        if (combinedResults.isEmpty()) {
            tableModel.addRow(new Object[]{"Ничего не найдено", "", ""});
            return;
        }

        List<Map.Entry<String, Double>> resultList = new ArrayList<>(combinedResults.entrySet());

        if (sortBox.getSelectedIndex() == 0) {
            // По убыванию веса TF-IDF
            resultList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        } else {
            resultList.sort((e1, e2) -> {
                String name1 = Paths.get(e1.getKey()).getFileName().toString();
                String name2 = Paths.get(e2.getKey()).getFileName().toString();
                return name1.compareToIgnoreCase(name2);
            });
        }

        for (Map.Entry<String, Double> entry : resultList) {
            String fullPath = entry.getKey();
            String fileName = Paths.get(fullPath).getFileName().toString();
            String formattedScore = String.format("%.4f", entry.getValue());

            tableModel.addRow(new Object[]{fileName, formattedScore, fullPath});
        }
    }

    private void openSelectedFile() {
        int row = resultTable.getSelectedRow();
        String absolutePath = (String) tableModel.getValueAt(row, 2);

        if (absolutePath.isEmpty()) return;

        try {
            File file = new File(absolutePath);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this, "Файл больше не существует по этому пути!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Не удалось открыть файл. Проверьте права доступа.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}