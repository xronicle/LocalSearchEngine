import java.util.HashMap;
import java.util.Map;

public class InvertedIndex {
    private Map<String, Map<String, Integer>> index;

    public InvertedIndex() {
        this.index = new HashMap<>();
    }

    public void addWord(String word, String documentName) {
        if (word.isEmpty()) return;

        index.putIfAbsent(word, new HashMap<>());


        Map<String, Integer> documentFrequency = index.get(word);

        documentFrequency.put(documentName, documentFrequency.getOrDefault(documentName, 0) + 1);
    }

    public Map<String, Integer> search(String word) {
        return index.getOrDefault(word, new HashMap<>());
    }

    public int getSize() {
        return index.size();
    }
}