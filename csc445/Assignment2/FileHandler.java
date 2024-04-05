package Assignment2;

/*
    Tristan Allen
    CSC445 Assignment2
    Suny Oswego

    program to handle different file needs
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileHandler {

    public static void writeToFile(String content, String filename) {
        try {
            String directoryPath = "src/Assignment2/data/temp";
            String filepath = directoryPath + "/" + filename;

            File directory = new File(directoryPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            FileWriter writer = new FileWriter(filepath);
            writer.write(content);
            System.out.println("\nSuccessfully saved file. ");
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
