import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

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
    private JLabel currentFolderLabel;
    private InvertedIndex engine;
    private boolean isDarkMode = true;
    private final String INDEX_FILE = "index.json";

    public SearchEngineGUI() {
        engine = new InvertedIndex();

        setTitle("Локальная Поисковая Система");
        setSize(950, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topStatusPanel = new JPanel(new BorderLayout());
        currentFolderLabel = new JLabel("Текущая база: загрузка...");
        currentFolderLabel.setForeground(Color.GRAY);
        topStatusPanel.add(currentFolderLabel, BorderLayout.WEST);

        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton themeButton = new JButton();
        themeButton.setToolTipText("Сменить тему (Светлая/Темная)");
        themeButton.setFocusable(false);

        JButton helpButton = new JButton();
        helpButton.setToolTipText("Справка о программе");
        helpButton.setFocusable(false);

        Icon sunIcon = createSunIcon();
        Icon moonIcon = createMoonIcon();
        Icon helpIcon = createHelpIcon();

        themeButton.setIcon(sunIcon);
        helpButton.setIcon(helpIcon);

        iconPanel.add(themeButton);
        iconPanel.add(helpButton);
        topStatusPanel.add(iconPanel, BorderLayout.EAST);

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchField.putClientProperty("JTextField.placeholderText", "Введите запрос для поиска...");

        JPanel mainButtonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        JButton searchButton = new JButton("Найти");
        JButton chooseFolderButton = new JButton("Выбрать папку");
        mainButtonPanel.add(searchButton);
        mainButtonPanel.add(chooseFolderButton);

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(mainButtonPanel, BorderLayout.EAST);

        topPanel.add(topStatusPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);

        loadDatabaseIfExists();

        tableModel = new DefaultTableModel(new String[]{"Документ", "Цитата", "Совпадений", "Вес (TF-IDF)", "Скрытый путь"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Integer.class;
                if (columnIndex == 3) return Double.class;
                return String.class;
            }
        };

        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        resultTable.setRowHeight(50);
        resultTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        resultTable.setAutoCreateRowSorter(true);

        resultTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(100);

        resultTable.getColumnModel().getColumn(4).setMinWidth(0);
        resultTable.getColumnModel().getColumn(4).setMaxWidth(0);
        resultTable.getColumnModel().getColumn(4).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(e -> performSearch());
        searchField.addActionListener(e -> performSearch());
        chooseFolderButton.addActionListener(e -> selectFolderAndIndex());
        helpButton.addActionListener(e -> showHelpDialog());

        themeButton.addActionListener(e -> {
            isDarkMode = !isDarkMode;
            try {
                if (isDarkMode) {
                    UIManager.setLookAndFeel(new FlatDarkLaf());
                    themeButton.setIcon(sunIcon);
                } else {
                    UIManager.setLookAndFeel(new FlatLightLaf());
                    themeButton.setIcon(moonIcon);
                }
                SwingUtilities.updateComponentTreeUI(this);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

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
                currentFolderLabel.setText("Текущая база: " + engine.getRootFolderPath());
            } catch (Exception ignored) {
                currentFolderLabel.setText("Текущая база: Ошибка чтения кэша");
            }
        } else {
            currentFolderLabel.setText("Текущая база: Не выбрана");
        }
    }

    private void selectFolderAndIndex() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите папку для индексации");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = chooser.getSelectedFile();
            currentFolderLabel.setText("Текущая база: " + selectedFolder.getAbsolutePath());

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{"Индексация...", "", 0, 0.0, ""});

            new Thread(() -> {
                engine = new InvertedIndex();
                try (Stream<Path> paths = Files.list(selectedFolder.toPath())) {
                    paths.filter(Files::isRegularFile)
                            .filter(p -> {
                                String name = p.toString().toLowerCase();
                                return !name.startsWith("~$") && (name.endsWith(".txt") || name.endsWith(".pdf") || name.endsWith(".docx") || name.endsWith(".xlsx") || name.endsWith(".pptx"));
                            })
                            .forEach(filePath -> {
                                try {
                                    String content = FileParser.extractText(filePath);
                                    content = content.toLowerCase().replaceAll("[^a-zа-яё0-9\\s]", "");
                                    String[] words = content.split("\\s+");

                                    String absolutePath = filePath.toAbsolutePath().toString();
                                    for (String word : words) {
                                        if (word.isEmpty()) continue;
                                        engine.addWord(RussianStemmer.stem(word), absolutePath);
                                    }
                                } catch (Exception ex) {
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
        Map<String, Double> combinedTfIdf = new java.util.HashMap<>();
        Map<String, Integer> combinedFreq = new java.util.HashMap<>();

        String firstStemmedWord = "";

        for (String qWord : queryWords) {
            if (qWord.isEmpty()) continue;
            String stemmedQuery = RussianStemmer.stem(qWord);
            if (firstStemmedWord.isEmpty()) firstStemmedWord = stemmedQuery;

            Map<String, Double> tfIdfResults = engine.searchTfIdf(stemmedQuery);
            for (Map.Entry<String, Double> entry : tfIdfResults.entrySet()) {
                String fullPath = entry.getKey();
                combinedTfIdf.put(fullPath, combinedTfIdf.getOrDefault(fullPath, 0.0) + entry.getValue());
            }

            Map<String, Integer> freqResults = engine.getWordFrequencies(stemmedQuery);
            for (Map.Entry<String, Integer> entry : freqResults.entrySet()) {
                String fullPath = entry.getKey();
                combinedFreq.put(fullPath, combinedFreq.getOrDefault(fullPath, 0) + entry.getValue());
            }
        }

        tableModel.setRowCount(0);

        if (combinedTfIdf.isEmpty()) {
            tableModel.addRow(new Object[]{"Ничего не найдено", "", 0, 0.0, ""});
            return;
        }

        List<String> resultPaths = new ArrayList<>(combinedTfIdf.keySet());

        for (String fullPath : resultPaths) {
            String fileName = Paths.get(fullPath).getFileName().toString();
            int freq = combinedFreq.getOrDefault(fullPath, 0);
            double rawScore = combinedTfIdf.get(fullPath);
            double formattedScore = Math.round(rawScore * 10000.0) / 10000.0;

            String snippet = SnippetGenerator.generateSnippet(fullPath, firstStemmedWord, freq);

            tableModel.addRow(new Object[]{fileName, snippet, freq, formattedScore, fullPath});
        }

        resultTable.getRowSorter().toggleSortOrder(3);
        resultTable.getRowSorter().toggleSortOrder(3);
    }

    private void openSelectedFile() {
        int row = resultTable.getSelectedRow();
        if (row == -1) return;

        int modelRow = resultTable.convertRowIndexToModel(row);
        String absolutePath = (String) tableModel.getValueAt(modelRow, 4);

        if (absolutePath.isEmpty()) return;

        try {
            File file = new File(absolutePath);
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            } else {
                JOptionPane.showMessageDialog(this, "Файл больше не существует по этому пути!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Не удалось открыть файл.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showHelpDialog() {
        String helpText = "<html><body style='width: 450px; font-family: sans-serif;'>" +
                "<h2 style='color: #4A90E2;'>Как работает этот поисковик?</h2>" +
                "<p><b>1. Морфологический анализ (Стемминг)</b><br>" +
                "Программа не ищет точные совпадения. Алгоритм Портера отсекает окончания и суффиксы. " +
                "Запрос «электротехнику» превращается в корень «электротехник», поэтому поисковик " +
                "понимает любые падежи в документах.</p>" +
                "<p><b>2. Метрика релевантности TF-IDF</b><br>" +
                "Ранжирование происходит по математической модели <i>Term Frequency - Inverse Document Frequency</i>.<br>" +
                "Частые слова-паразиты (предлоги, союзы) автоматически получают нулевой вес. " +
                "Редкие и узкоспециализированные термины получают высший балл, поднимая релевантные файлы в топ.</p>" +
                "<p><b>3. Инвертированный индекс</b><br>" +
                "Сырой текст из бинарных PDF и DOCX извлекается (Apache POI/PDFBox) и кэшируется в локальную базу данных (JSON). При поиске программа " +
                "мгновенно находит результат в памяти, не перечитывая жесткий диск заново.</p>" +
                "<br><p style='text-align: right; color: gray;'><i>Разработано: М.А. Коротков, TPU, 2026</i></p>" +
                "</body></html>";

        JOptionPane.showMessageDialog(this, helpText, "О технологии", JOptionPane.INFORMATION_MESSAGE);
    }

    private static Icon createSunIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getForeground());

                int centerX = x + getIconWidth() / 2;
                int centerY = y + getIconHeight() / 2;
                int radius = 5;

                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

                int rayInner = radius + 2;
                int rayOuter = radius + 4;
                for (int i = 0; i < 8; i++) {
                    double angle = i * Math.PI / 4;
                    int x1 = centerX + (int) (Math.cos(angle) * rayInner);
                    int y1 = centerY + (int) (Math.sin(angle) * rayInner);
                    int x2 = centerX + (int) (Math.cos(angle) * rayOuter);
                    int y2 = centerY + (int) (Math.sin(angle) * rayOuter);
                    g2.drawLine(x1, y1, x2, y2);
                }
                g2.dispose();
            }
            @Override public int getIconWidth() { return 24; }
            @Override public int getIconHeight() { return 24; }
        };
    }

    private static Icon createMoonIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getForeground());

                int centerX = x + getIconWidth() / 2;
                int centerY = y + getIconHeight() / 2;
                int size = 12;

                g2.fillOval(centerX - size/2, centerY - size/2, size, size);
                g2.setColor(c.getBackground());
                g2.fillOval(centerX - size/2 + 4, centerY - size/2 - 2, size, size);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 24; }
            @Override public int getIconHeight() { return 24; }
        };
    }

    private static Icon createHelpIcon() {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getForeground());

                int centerX = x + getIconWidth() / 2;
                int centerY = y + getIconHeight() / 2;
                int size = 16;

                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(centerX - size/2, centerY - size/2, size, size);

                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth("?");
                int textHeight = fm.getAscent();
                g2.drawString("?", centerX - textWidth/2, centerY + textHeight/2 - 1);
                g2.dispose();
            }
            @Override public int getIconWidth() { return 24; }
            @Override public int getIconHeight() { return 24; }
        };
    }
}