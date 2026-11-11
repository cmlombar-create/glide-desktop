package glide;

import glide.dao.DatabaseManager;
import glide.ui.MainWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Primary application launcher for the Glide Schedule Manager (SDD Section 3.1).
 * Initializes the local SQLite database and launches the Swing UI on the Event Dispatch Thread (EDT).
 */
public final class GlideApp {
    public static void main(String[] args) {
        try {
            // Initialize glide.db, verify the 7-column schema, and build the composite B-tree index
            DatabaseManager.initialize();
        } catch (Exception e) {
            // Display a user-friendly fatal startup alert if the SQLite file is locked or corrupted (SDD Table 4-7)
            JOptionPane.showMessageDialog(null,
                "Cannot initialize Glide database:\n" + e.getMessage(),
                "Glide", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Launch the MainWindow dashboard safely on the Java Swing Event Dispatch Thread (SDD Section 8.2)
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}