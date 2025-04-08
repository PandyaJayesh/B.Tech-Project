package com.example.wordcount;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class FileSplitter {

    private static final int BUFFER_SIZE = 1000; // Number of lines stored in memory at once

    public static List<String> splitTextFile(String fileName, int numberOfSubfiles) throws IOException, InterruptedException {
        List<String> subfileNames = new ArrayList<>();
        File file = new File(fileName);

        // Thread-safe queue to distribute lines
        BlockingQueue<String> lineQueue = new LinkedBlockingQueue<>(BUFFER_SIZE);
        ExecutorService writerPool = Executors.newFixedThreadPool(numberOfSubfiles);

        // Create subfile writers
        for (int i = 0; i < numberOfSubfiles; i++) {
            String subfileName = fileName.replace(".txt", "_" + i + ".txt");
            subfileNames.add(subfileName);
            writerPool.execute(new FileWriterTask(subfileName, lineQueue));
        }

        // Read file and distribute lines
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineQueue.put(line); // Blocking call ensures proper synchronization
            }
        }

        // Signal end of processing
        for (int i = 0; i < numberOfSubfiles; i++) {
            lineQueue.put("EOF");
        }

        // Shutdown writer pool after completion
        writerPool.shutdown();
        writerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        return subfileNames;
    }

    static class FileWriterTask implements Runnable {
        private final String subfileName;
        private final BlockingQueue<String> lineQueue;

        public FileWriterTask(String subfileName, BlockingQueue<String> lineQueue) {
            this.subfileName = subfileName;
            this.lineQueue = lineQueue;
        }

        @Override
        public void run() {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(subfileName))) {
                while (true) {
                    String line = lineQueue.take();
                    if ("EOF".equals(line)) break;  // Stop writing if EOF signal is received
                    writer.write(line);
                    writer.newLine();
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    public static List<String> splitTextFile(String fileName, int numberOfSubfiles) throws IOException {
//        List<String> subfileNames = new ArrayList<>();
//        File file = new File(fileName);
//
//        // Determine total number of lines
//        int totalLines = countLines(file);
//        int linesPerSubfile = (int) Math.ceil((double) totalLines / numberOfSubfiles);
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//            for (int i = 0; i < numberOfSubfiles; i++) {
//                String subfileName = fileName.replace(".txt", "_" + i + ".txt");
//                subfileNames.add(subfileName);
//
//                try (BufferedWriter writer = new BufferedWriter(new FileWriter(subfileName))) {
//                    for (int j = 0; j < linesPerSubfile; j++) {
//                        String line = reader.readLine();
//                        if (line == null) break;  // Stop if end of file reached
//                        writer.write(line);
//                        writer.newLine();
//                    }
//                }
//            }
//        }
//        return subfileNames;
//    }
//
//    private static int countLines(File file) throws IOException {
//        int lines = 0;
//        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//            while (reader.readLine() != null) lines++;
//        }
//        return lines;
//    }

//    public static List<String> splitTextFile(String fileName, int numberOfSubfiles) throws IOException {
//        List<String> subfileNames = new ArrayList<>();
//        File file = new File(fileName);
//        StringBuilder stringBuilder = new StringBuilder();
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
//            String line;
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line).append("\n");
//            }
//        }
//
//        String content = stringBuilder.toString();
//        int totalLength = content.length();
//        int subfileSize = totalLength / numberOfSubfiles;
//
//
//        for (int i = 0; i < numberOfSubfiles; i++) {
//            int startIndex = i * subfileSize;
//            int endIndex = (i == numberOfSubfiles - 1) ? totalLength : startIndex + subfileSize;
//            String subfileContent = content.substring(startIndex, endIndex);
//
//
//            // Write subfile to disk
//            String subfileName = fileName.replace(".txt", "_" + i + ".txt");
//            subfileNames.add(subfileName);
//            writeToFile(subfileName, subfileContent);
//        }
//        return subfileNames;
//    }

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
