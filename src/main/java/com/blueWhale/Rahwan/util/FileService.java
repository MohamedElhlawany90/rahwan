package com.blueWhale.Rahwan.util;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {

    public static boolean deleteFileByName(String fileName, String directoryPath) {
        try {
            // Construct the full path
            Path filePath = Paths.get(directoryPath, fileName);

            // Check if file exists
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            } else {
                return false; // File does not exist
            }
        } catch (IOException e) {
            return false;
            // throw new RuntimeException("Failed to delete file: " + fileName, e);
        }
    }
}