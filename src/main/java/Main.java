import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.out.println("Не удалось загрузить современную тему.");
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            SearchEngineGUI gui = new SearchEngineGUI();
            gui.setVisible(true);
        });
    }
}