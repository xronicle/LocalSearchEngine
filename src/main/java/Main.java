import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("Не удалось загрузить системный дизайн окон.");
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            SearchEngineGUI gui = new SearchEngineGUI();
            gui.setVisible(true);
        });
    }
}