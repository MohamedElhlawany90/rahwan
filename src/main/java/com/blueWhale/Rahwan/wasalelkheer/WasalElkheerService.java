package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.charity.Charity;
import com.blueWhale.Rahwan.charity.CharityRepository;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
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
public class WasalElkheerService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final WasalElkheerRepository WasalElkheerRepository;
    private final WasalElkheerMapper WasalElkheerMapper;
    private final UserRepository userRepository;
    private final CharityRepository charityRepository;

    public WasalElkheerDto createWasalElkheer(WasalElkheerForm form, UUID userId) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is not active");
        }

        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + form.getCharityId()));

        if (!charity.isActive()) {
            throw new RuntimeException("Charity is not active");
        }

        WasalElkheer WasalElkheer = WasalElkheerMapper.toEntity(form);
        WasalElkheer.setUserId(userId);

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getPhoto() != null) {
            // لو الفولدر مش موجود نعمله
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(form.getPhoto().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + form.getPhoto().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            WasalElkheer.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }
        WasalElkheer.setStatus(WasalElkheerStatus.PENDING);

        WasalElkheer saved = WasalElkheerRepository.save(WasalElkheer);
        return enrichDto(WasalElkheerMapper.toDto(saved));
    }

    public List<WasalElkheerDto> getUserOrders(UUID userId) {
        return WasalElkheerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(WasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getCharityOrders(Long charityId) {
        return WasalElkheerRepository.findByCharityIdOrderByCreatedAtDesc(charityId)
                .stream()
                .map(WasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getOrdersByStatus(WasalElkheerStatus status) {
        return WasalElkheerRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(WasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public WasalElkheerDto getOrderById(Long orderId) {
        WasalElkheer WasalElkheer = WasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return enrichDto(WasalElkheerMapper.toDto(WasalElkheer));
    }

    public WasalElkheerDto updateOrderStatus(Long orderId, WasalElkheerStatus newStatus) {
        WasalElkheer WasalElkheer = WasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        WasalElkheer.setStatus(newStatus);
        WasalElkheer updated = WasalElkheerRepository.save(WasalElkheer);
        return enrichDto(WasalElkheerMapper.toDto(updated));
    }

    public List<WasalElkheerDto> getAllOrders() {
        return WasalElkheerRepository.findAll()
                .stream()
                .map(WasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    private WasalElkheerDto enrichDto(WasalElkheerDto dto) {
        if (dto.getUserId() != null) {
            userRepository.findById(dto.getUserId()).ifPresent(user ->
                    dto.setUserName(user.getName())
            );
        }

        if (dto.getCharityId() != null) {
            charityRepository.findById(dto.getCharityId()).ifPresent(charity -> {
                dto.setCharityNameAr(charity.getNameAr());
                dto.setCharityNameEn(charity.getNameEn());
            });
        }

        return dto;
    }
}