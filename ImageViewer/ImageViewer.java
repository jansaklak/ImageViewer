import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Queue;
import java.util.*;

public class ImageViewer extends JFrame {
    private JLabel imageLabel;
    private File[] imageFiles;
    private int currentIndex = 0;
    int img_length = 0;
    int img_choosen = 0;
    double ratio_choosen;
    byte[] buffer = new byte[1024];
    Queue<Integer> imageQueue;
    private SimpleDateFormat dateFormat;
    private boolean showMultipleMode = false;
    private long lastKeyPressTime = 0;
    int currIndex;
    private static final long KEY_PRESS_DELAY = 200; // Czas opóźnienia w milisekundach
    public ImageViewer(String folderPath) {
        super("Image Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        File folder = new File(folderPath);
        imageFiles = folder.listFiles();
        img_length = imageFiles.length;

        imageQueue = new LinkedList<Integer>();
        for (int i = 0; i < imageFiles.length; i++) {
            imageQueue.add(i);
        }
        Arrays.sort(imageFiles, new ImageFileComparator());
        imageLabel = new JLabel();
        add(imageLabel);
        setVisible(true);
        dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        showNextImage();
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                char key = e.getKeyChar();
                if (key == 'y') {
                    img_choosen++;
                    try {
                        moveCurrentImage(folderPath + "/Wybrane");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    showNextImage();
                    ratio_choosen = (img_choosen * 100) / img_length;
                } else if (key == 'n') {
                    try {
                        moveCurrentImage(folderPath + "/Odrzucone");
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    showNextImage();
                } else if (key == 'm') {
                    if (!showMultipleMode) {
                        showMultipleImages();
                    }
                } else if (key == 'q') {
                    System.exit(0);
                } else if (key == 'e') {
                    exitMultipleMode();
                } else if (key == 's') {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - lastKeyPressTime;
                    if (elapsedTime >= KEY_PRESS_DELAY) {
                        lastKeyPressTime = currentTime;
                        System.out.println("lista przed");
                        System.out.println(imageQueue);
                        imageQueue.add(currIndex);
                        System.out.println("lista po");
                        System.out.println(imageQueue);
                        showNextImage();
                    }
                }
            }
        });
    }
    private void moveCurrentImage(String folderName) throws IOException {
        File currentImage = imageFiles[currIndex];
        new File(folderName).mkdirs();
        File kopiowane = new File(folderName + "/" + currentImage.getName());
        FileInputStream in = new FileInputStream(currentImage);
        FileOutputStream out = new FileOutputStream(folderName + "/" + currentImage.getName());
        String c;
        int totalSize;
        while (((totalSize = in.read(buffer)) > 0)) {
            out.write(buffer, 0, totalSize);
        }
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    private void showNextImage() {
        System.out.println(imageQueue);

        if (imageQueue.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nie ma więcej zdjęć. Wybrano: " + (img_choosen * 100) / img_length + "%");
            System.exit(0);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            System.out.println("Wyświetlam kolejne");
            int imageIndex = imageQueue.poll();
            currIndex = imageIndex;
            File currentImage = imageFiles[imageIndex];
            System.out.println("Wysywietlam " + currentImage.getName());
            setTitle(currentImage.getName());
            if(!(currentImage.isFile() && (currentImage.getName().endsWith("jpg") || currentImage.getName().endsWith("JPG")|| currentImage.getName().endsWith("png")|| currentImage.getName().endsWith("PNG")))){
                System.out.println("Wykryto folder");
                currentIndex++;
                showNextImage();
                return;
            }

            ImageIcon icon = new ImageIcon(currentImage.getPath());
            Image image = icon.getImage();
            int maxWidth = getWidth() - getInsets().left - getInsets().right;
            int maxHeight = getHeight() - getInsets().top - getInsets().bottom;
            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);
            double scaleFactor = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
            int scaledWidth = (int) (originalWidth * scaleFactor);
            int scaledHeight = (int) (originalHeight * scaleFactor);
            Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
            JPanel imagePanel = new JPanel(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;
            constraints.anchor = GridBagConstraints.CENTER;
            imagePanel.add(imageLabel, constraints);
            getContentPane().removeAll();
            getContentPane().add(imagePanel, BorderLayout.CENTER);
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(currentImage);
                ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                Date dateTaken = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (dateTaken != null) {
                    String dateString = dateFormat.format(dateTaken);
                    String dayOfWeek = new SimpleDateFormat("EEEE").format(dateTaken);
                    dateString += " (" + dayOfWeek + ")";
                    JPanel datePanel = new JPanel();
                    JLabel dateLabel = new JLabel("Zrobiono: " + dateString);
                    Font font = new Font(dateLabel.getFont().getName(), Font.PLAIN, 18);
                    dateLabel.setFont(font);
                    datePanel.add(dateLabel);
                    add(datePanel, BorderLayout.NORTH);
                }
            } catch (Exception e) {
                System.out.println("Failed to extract date taken from image: " + currentImage.getName());
            }
            pack();
            currentIndex++;
        }
    }
    private void showMultipleImages() {

        int imageIndex = imageQueue.peek() - 1;
        if(imageIndex < 0) imageIndex = 0;
        if (!showMultipleMode && imageIndex + 3 < imageFiles.length) {

            Queue<Integer> pobierz = new LinkedList<Integer>();
            pobierz.add(currIndex);
            pobierz.addAll(imageQueue);
            showMultipleMode = true;
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            JPanel imagesPanel = new JPanel(new GridLayout(2, 2));

            for (int i = 0; i < 4; i++) {
                File currentImage = imageFiles[pobierz.poll()];

                ImageIcon icon = new ImageIcon(currentImage.getPath());
                Image image = icon.getImage();

                int maxWidth = getWidth() / 2 - getInsets().left - getInsets().right;
                int maxHeight = getHeight() / 2 - getInsets().top - getInsets().bottom;
                int originalWidth = image.getWidth(null);
                int originalHeight = image.getHeight(null);
                double scaleFactor = Math.min((double) maxWidth / originalWidth, (double) maxHeight / originalHeight);
                int scaledWidth = (int) (originalWidth * scaleFactor);
                int scaledHeight = (int) (originalHeight * scaleFactor);

                Image scaledImage = image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaledImage));
                JPanel imagePanel = new JPanel(new GridBagLayout());
                GridBagConstraints constraints = new GridBagConstraints();

                constraints.gridx = 0;
                constraints.gridy = 0;
                constraints.weightx = 1.0;
                constraints.weighty = 1.0;
                constraints.anchor = GridBagConstraints.CENTER;

                imagePanel.add(imageLabel, constraints);
                imagesPanel.add(imagePanel);
            }
            getContentPane().removeAll();
            getContentPane().add(imagesPanel, BorderLayout.CENTER);
            pack();
        } else {
        }
    }
    private void exitMultipleMode() {
        if (showMultipleMode) {
            showMultipleMode = false;
            setExtendedState(JFrame.MAXIMIZED_BOTH);
            Queue<Integer> kol = new LinkedList<Integer>();
            System.out.println(imageQueue);
            kol.add(currIndex);
            kol.addAll(imageQueue);
            imageQueue = kol;
            showNextImage();
        }
    }
    private static class ImageFileComparator implements Comparator<File> {
        @Override
        public int compare(File file1, File file2) {
            try {

                Metadata metadata1 = ImageMetadataReader.readMetadata(file1);
                Metadata metadata2 = ImageMetadataReader.readMetadata(file2);

                ExifSubIFDDirectory directory1 = metadata1.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
                ExifSubIFDDirectory directory2 = metadata2.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

                Date dateTaken1 = directory1.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                Date dateTaken2 = directory2.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

                if (dateTaken1 != null && dateTaken2 != null) {
                    return dateTaken1.compareTo(dateTaken2);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (args.length > 0) {
                new ImageViewer(args[0]);
            } else {
                JOptionPane.showMessageDialog(null, "Nie podano ścieżki do folderu z obrazami.");
            }
        });
    }
}