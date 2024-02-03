import javax.swing.*;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            Path selectedFolder = fileChooser.getSelectedFile().toPath();
            String folderPath = selectedFolder.toAbsolutePath().toString();
            new ImageViewer(folderPath);
        }
    }
}
