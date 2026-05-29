import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

public class SearchEngineGUI extends JFrame {
    private JTextField searchField;
    private JTextArea resultArea;
    private InvertedIndex engine;

    public SearchEngineGUI(InvertedIndex engine) {
        this.engine = engine;

        setTitle("Локальная Поисковая Система");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Отступы

        searchField = new JTextField();
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JButton searchButton = new JButton("Найти");
        searchButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(resultArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });

        searchField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performSearch();
            }
        });
    }

    private void performSearch() {
        String query = searchField.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            resultArea.setText("Пожалуйста, введите запрос для поиска.");
            return;
        }

        resultArea.setText("Идет поиск...\n\n");

        String[] queryWords = query.split("\\s+");
        Map<String, Double> combinedResults = new java.util.HashMap<>();

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
            StringBuilder sb = new StringBuilder();
            sb.append("Результаты поиска (по убыванию релевантности):\n");
            sb.append("--------------------------------------------------\n\n");

            combinedResults.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        sb.append(String.format(" \uD83D\uDCC4 %s\n      [TF-IDF Вес: %.4f]\n\n", entry.getKey(), entry.getValue()));
                    });

            resultArea.setText(sb.toString());
        } else {
            resultArea.setText("По вашему запросу ничего не найдено.");
        }
    }
}