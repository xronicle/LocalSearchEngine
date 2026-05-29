import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.FileWriter;

public class InvertedIndex {

    private Map<String, Map<String, Integer>> index;
    private Set<String> indexedDocuments;

    public InvertedIndex() {
        this.index = new HashMap<>();
        this.indexedDocuments = new HashSet<>();
    }

    public void addWord(String word, String documentName) {
        if (word.isEmpty()) return;

        indexedDocuments.add(documentName);

        index.putIfAbsent(word, new HashMap<>());
        Map<String, Integer> documentFrequency = index.get(word);
        documentFrequency.put(documentName, documentFrequency.getOrDefault(documentName, 0) + 1);
    }

    public Map<String, Double> searchTfIdf(String word) {
        Map<String, Double> results = new HashMap<>();

        if (!index.containsKey(word)) {
            return results;
        }

        Map<String, Integer> tfMap = index.get(word);

        double n = indexedDocuments.size();
        double df = tfMap.size();

        double idf = Math.log10(n / df);

        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String documentName = entry.getKey();
            int tf = entry.getValue();

            double tfIdf = tf * idf;
            results.put(documentName, tfIdf);
        }

        return results;
    }

    public int getSize() {
        return index.size();
    }
        public void saveToFile(String filePath) throws Exception {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(filePath)) {
                gson.toJson(this, writer);
        }
    }

        public static InvertedIndex loadFromFile(String filePath) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, InvertedIndex.class);
        }
    }

    public Map<String, Integer> getWordFrequencies(String word) {
        return index.getOrDefault(word, new java.util.HashMap<>());
    }

    public String getRootFolderPath() {
        if (indexedDocuments == null || indexedDocuments.isEmpty()) {
            return "База пуста";
        }
        String firstPath = indexedDocuments.iterator().next();
        try {
            return java.nio.file.Paths.get(firstPath).getParent().toString();
        } catch (Exception e) {
            return "Неизвестная директория";
        }
    }
}