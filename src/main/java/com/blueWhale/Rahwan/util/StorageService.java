package com.blueWhale.Rahwan.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsible for one thing only: saving uploaded files to disk.
 */
@Service
public class StorageService {

    @Value("${app.upload.dir:/home/ubuntu/rahwan/}")
    private String uploadDir;

    /**
     * Saves a multipart file to the configured upload directory.
     * @return the saved filename (stored in the DB as a reference)
     */
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return null;

        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) Files.createDirectories(dir);

        String fileName = new Date().getTime() + "A-A" + file.getOriginalFilename();
        Path target = dir.resolve(fileName);

        byte[] bytes = ImageUtility.compressImage(file.getBytes());
        Files.write(target, bytes);
        setReadPermissions(target);

        return fileName;
    }

    private void setReadPermissions(Path path) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.OTHERS_READ);
        Files.setPosixFilePermissions(path, perms);
    }
}