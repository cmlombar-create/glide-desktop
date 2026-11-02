package glide;

import glide.dao.DatabaseManager;
import glide.ui.MainWindow;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class GlideApp {
    public static void main(String[] args) {
        try { DatabaseManager.initialize(); }
        catch(Exception e) {
            JOptionPane.showMessageDialog(null,"Cannot initialize Glide database:\n"+e.getMessage(),"Glide",JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
