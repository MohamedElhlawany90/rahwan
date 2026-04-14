package com.blueWhale.Rahwan.charity;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.util.ImageUtility;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CharityService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final CharityRepository charityRepository;
    private final CharityMapper charityMapper;

    public CharityDto createCharity(CharityForm form) throws IOException {

        validatePhone(form.getPhone());

        charityRepository.findByPhone(form.getPhone())
                .ifPresent(charity -> {
                    throw new RuntimeException("Phone already exists: " + form.getPhone());
                });

        Charity charity = charityMapper.toEntity(form);

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getLogo() != null) {
            // لو الفولدر مش موجود نعمله
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(form.getLogo().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + form.getLogo().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            charity.setLogo(url.substring(url.lastIndexOf("/") + 1));
        }

        Charity saved = charityRepository.save(charity);
        return charityMapper.toDto(saved);
    }

    public List<CharityDto> getAllActiveCharities() {
        return charityRepository.findByActiveTrue()
                .stream()
                .map(charityMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<CharityDto> getAllCharities() {
        return charityRepository.findAll()
                .stream()
                .map(charityMapper::toDto)
                .collect(Collectors.toList());
    }

    public CharityDto getCharityById(Long id) {
        Charity charity = charityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + id));
        return charityMapper.toDto(charity);
    }

    public CharityDto updateCharity(Long id, CharityForm form) throws IOException {

        Charity charity = charityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + id));

        validatePhone(form.getPhone());

        charityRepository.findByPhone(form.getPhone())
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> {
                    throw new RuntimeException("Phone already exists");
                });

        charity.setNameAr(form.getNameAr());
        charity.setNameEn(form.getNameEn());
        charity.setDescriptionAr(form.getDescriptionAr());
        charity.setDescriptionEn(form.getDescriptionEn());
        charity.setPhone(form.getPhone());
        charity.setLatitude(form.getLatitude());
        charity.setLongitude(form.getLongitude());

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getLogo() != null) {
            // لو الفولدر مش موجود نعمله
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(form.getLogo().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + form.getLogo().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            charity.setLogo(url.substring(url.lastIndexOf("/") + 1));
        }

        Charity updated = charityRepository.save(charity);
        return charityMapper.toDto(updated);
    }

    public void deleteCharity(Long id) {
        Charity charity = charityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + id));

        charity.setActive(false);
        charityRepository.save(charity);
    }

    public CharityDto reactivateCharity(Long id) {
        Charity charity = charityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + id));

        charity.setActive(true);
        Charity updated = charityRepository.save(charity);
        return charityMapper.toDto(updated);
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.startsWith("20") || phone.length() != 12) {
            throw new RuntimeException("Phone must start with 20 and be 12 digits");
        }
    }
}