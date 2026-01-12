package com.blueWhale.Rahwan.advertisement;

import com.blueWhale.Rahwan.advertisement.Advertisement;
import com.blueWhale.Rahwan.advertisement.AdvertisementMapper;
import com.blueWhale.Rahwan.advertisement.AdvertisementRepository;
import com.blueWhale.Rahwan.util.ImageUtility;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AdvertisementService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final AdvertisementRepository repository;
    private final AdvertisementMapper mapper;

    public AdvertisementService(AdvertisementRepository repository,
                                AdvertisementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public AdvertisementDto create(AdvertisementForm advertisementForm) throws IOException {

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        Advertisement advertisement = mapper.toEntity(advertisementForm);

        if (advertisementForm.getPhoto() != null) {
               // لو الفولدر مش موجود نعمله
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(advertisementForm.getPhoto().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + advertisementForm.getPhoto().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            advertisement.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }

        Advertisement saved = repository.save(advertisement);
        return mapper.toDto(saved);
    }

    public AdvertisementDto update(String id, AdvertisementForm advertisementForm) throws IOException {

        Advertisement advertisement = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        mapper.updateEntity(advertisementForm, advertisement);

        if (advertisementForm.getPhoto() != null) {
            byte[] bytes = ImageUtility.compressImage(advertisementForm.getPhoto().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + advertisementForm.getPhoto().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            advertisement.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }

        Advertisement saved = repository.save(advertisement);
        return mapper.toDto(saved);
    }

    public List<AdvertisementDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    private String savePhoto(MultipartFile photo) {
        // منطق حفظ الصورة في نظام الملفات أو خدمة تخزين سحابية
        // وإرجاع اسم الملف أو الرابط
        return photo.getOriginalFilename(); // مثال بسيط
    }
}