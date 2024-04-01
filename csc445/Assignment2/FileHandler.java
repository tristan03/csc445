package Assignment2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileHandler {

    public static void saveFile(File downloadedFile) {
        String directoryPath = "src/Assignment2/data/temp/";
        File dir = new File(directoryPath);
        File file = new File(directoryPath, downloadedFile.getName());
        try {
            if (!dir.exists()) {
                dir.mkdir();
            }

            Files.copy(downloadedFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to save file ");
        }

        System.out.println("File saved at " + file.getAbsolutePath());
    }

    public static void writeToFile(String content, String senderID) {
        try {
            String directoryPath = "src/Assignment2/data/";
            String filename = senderID+ ".txt";
            String filepath = directoryPath + "/" + filename;

            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            FileWriter writer = new FileWriter(filepath);
            writer.write(content);
            System.out.println("Successfully saved file. ");
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFile(File file) {
        try {
            String filepath = "src/Assignment2/data/" + file.getName();
            return new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getFile(String filename) {
        String directoryPath = "src/Assignment2/data/";
        String filepath = directoryPath + "/" + filename;
        File file = new File(filepath);

        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }
}
