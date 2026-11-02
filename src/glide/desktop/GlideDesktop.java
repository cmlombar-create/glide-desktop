package glide.desktop;

import glide.dao.DatabaseManager;
import glide.ui.MainWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Existing Glide Desktop application entry point. */
public class GlideDesktop {
    public static void main(String[] args) {
        try {
            DatabaseManager.initialize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Cannot initialize Glide database:\n" + e.getMessage(),
                "Glide", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
