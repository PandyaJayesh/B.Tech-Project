package com.example.wordcount;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileSplitter {

    public static List<String> splitTextFile(String fileName, int numberOfSubfiles) throws IOException {
        List<String> subfileNames = new ArrayList<>();
        File file = new File(fileName);
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }

        String content = stringBuilder.toString();
        int totalLength = content.length();
        int subfileSize = totalLength / numberOfSubfiles;


        for (int i = 0; i < numberOfSubfiles; i++) {
            int startIndex = i * subfileSize;
            int endIndex = (i == numberOfSubfiles - 1) ? totalLength : startIndex + subfileSize;
            String subfileContent = content.substring(startIndex, endIndex);


            // Write subfile to disk
            String subfileName = fileName.replace(".txt", "_" + i + ".txt");
            subfileNames.add(subfileName);
            writeToFile(subfileName, subfileContent);
        }
        return subfileNames;
    }

    private static void writeToFile(String fileName, String content) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.print(content);
        }
    }

    public static List<String> splitTextFileBySize(String fileName, Map<String, Double> capacities) throws IOException {
        File file = new File(fileName);
        long totalSize = file.length();

        // Calculate total capacity (sum of speeds)
        double totalCapacity = capacities.values().stream().mapToDouble(Double::doubleValue).sum();
        List<String> subfileNames = new ArrayList<>();
        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
            // Distribute file portions based on capacities

            for (Map.Entry<String, Double> entry : capacities.entrySet()) {
                String clientIp = entry.getKey();
                double speed = entry.getValue();

                // Calculate the portion size in bytes for this IP
                long portionSize = (long) ((totalSize * speed) / totalCapacity);
                File portionFile = new File(fileName + "_" + clientIp + ".txt");

                subfileNames.add(portionFile.getAbsolutePath());

                try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(portionFile))) {
                    long writtenBytes = 0;
                    byte[] buffer = new byte[8192]; // 8 KB buffer
                    int bytesRead;

                    // Write up to the portion size
                    while (writtenBytes < portionSize
                            && (bytesRead = inputStream.read(buffer, 0, (int) Math.min(buffer.length, portionSize - writtenBytes))) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        writtenBytes += bytesRead;
                    }
                }
            }

            // Handle leftover content (if any)
            File remainingFile = new File(fileName + "_remaining.txt");
            try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(remainingFile))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                subfileNames.add(remainingFile.getAbsolutePath());
            }
        }

        return subfileNames;
    }
}
