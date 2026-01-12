//package com.blueWhale.Rahwan.order.service;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.UUID;
//
//@Service
//public class FileUploadService {
//
//    private static final String UPLOAD_DIR = "uploads/orders/";
//
//    public String saveOrderPhoto(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            return null;
//        }
//
//        try {
//            Path uploadPath = Paths.get(UPLOAD_DIR);
//            if (!Files.exists(uploadPath)) {
//                Files.createDirectories(uploadPath);
//            }
//
//            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            Path filePath = uploadPath.resolve(fileName);
//            Files.write(filePath, file.getBytes());
//
//            return fileName;
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to upload order photo: " + e.getMessage());
//        }
//    }
//}
