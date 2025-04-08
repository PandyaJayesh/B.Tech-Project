package com.example.wordcount;
// WordCount.java

import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class WordCount {

    public int countWords(String filePath) {
//        int wordCount = 0;
//        try {
//            // Open the file for reading
//            File file = new File(filePath);
//            FileReader fileReader = new FileReader(file);
//            BufferedReader bufferedReader = new BufferedReader(fileReader);
//
//            // Read each line from the file
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                // Split the line into words based on whitespace characters
//                String[] words = line.split("\\s+");
//                // Increment word count by the number of words in the current line
//                wordCount += words.length;
//            }
//
//            // Close readers
//            bufferedReader.close();
//            fileReader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.err.println("Error counting words: " + e.getMessage());
//        }
//        return wordCount;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
                ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
                return forkJoinPool.submit(() ->
                        lines.parallel()
                                .mapToInt(line -> line.split("\\s+").length)
                                .sum()
                ).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return 0;
            }
        }
        return 0;
    }
}