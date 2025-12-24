package com.blueWhale.Rahwan.advertisement;

import com.blueWhale.Rahwan.advertisement.Advertisement;
import com.blueWhale.Rahwan.advertisement.AdvertisementMapper;
import com.blueWhale.Rahwan.advertisement.AdvertisementRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Transactional
public class AdvertisementService {

    private final AdvertisementRepository repository;
    private final AdvertisementMapper mapper;

    public AdvertisementService(AdvertisementRepository repository,
                                AdvertisementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public AdvertisementDto create(AdvertisementForm form, MultipartFile photo) {

        Advertisement ad = mapper.toEntity(form);

        if (photo != null && !photo.isEmpty()) {
            String fileName = savePhoto(photo);
            ad.setPhoto(fileName);
        }

        Advertisement saved = repository.save(ad);
        return mapper.toDto(saved);
    }

    public AdvertisementDto update(String id, AdvertisementForm form, MultipartFile photo) {

        Advertisement ad = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Advertisement not found"));

        mapper.updateEntity(form, ad);

        if (photo != null && !photo.isEmpty()) {
            String fileName = savePhoto(photo);
            ad.setPhoto(fileName);
        }

        Advertisement saved = repository.save(ad);
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