package glide.desktop;

import glide.dao.DatabaseManager;
import glide.ui.MainWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * NetBeans project entry point for the Glide Desktop Schedule Manager (SDD Section 9.2).
 */
public class GlideDesktop {
    public static void main(String[] args) {
        try {
            // Bootstrap local SQLite database (glide.db), events table, and idx_active_events index
            DatabaseManager.initialize();
        } catch (Exception e) {
            // Alert the user and abort startup cleanly if database initialization fails
            JOptionPane.showMessageDialog(null,
                "Cannot initialize Glide database:\n" + e.getMessage(),
                "Glide", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Dispatch the main dashboard JFrame onto the Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}