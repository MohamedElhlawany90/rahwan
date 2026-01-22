package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.charity.Charity;
import com.blueWhale.Rahwan.charity.CharityRepository;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.order.CreationDto;
import com.blueWhale.Rahwan.order.Order;
import com.blueWhale.Rahwan.order.OrderDto;
import com.blueWhale.Rahwan.order.OrderStatus;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class WasalElkheerService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final WasalElkheerRepository wasalElkheerRepository;
    private final WasalElkheerMapper wasalElkheerMapper;
    private final UserRepository userRepository;
    private final CharityRepository charityRepository;
    private final WhatsAppService whatsAppService;

    public CreationWasalElkheerDto createWasalElkheer(WasalElkheerForm form, UUID userId) throws IOException {

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

        WasalElkheer wasalElkheer = wasalElkheerMapper.toEntity(form);
        wasalElkheer.setUserId(userId);

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
            wasalElkheer.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }
        wasalElkheer.setCreationStatus(CreationStatus.CREATED);

        WasalElkheer saved = wasalElkheerRepository.save(wasalElkheer);
        return enrichCreationDto(wasalElkheerMapper.toCreationWasalDto(saved));
    }

    public WasalElkheerDto confirmOrder(Long orderId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getCreationStatus() != CreationStatus.CREATED) {
            throw new RuntimeException("Order must be in Created status to confirm");
        }
        order.setStatus(WasalElkheerStatus.PENDING);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            whatsAppService.sendOrderConfirmation(user.getPhone());
        }
        return enrichDto(wasalElkheerMapper.toDto(updated));

    }

        public List<WasalElkheerDto> getUserOrders(UUID userId) {
        return wasalElkheerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(wasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getCharityOrders(Long charityId) {
        return wasalElkheerRepository.findByCharityIdOrderByCreatedAtDesc(charityId)
                .stream()
                .map(wasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getOrdersByStatus(WasalElkheerStatus status) {
        return wasalElkheerRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(wasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public WasalElkheerDto getOrderById(Long orderId) {
        WasalElkheer wasalElkheer = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return enrichDto(wasalElkheerMapper.toDto(wasalElkheer));
    }

    public WasalElkheerDto updateOrderStatus(Long orderId, WasalElkheerStatus newStatus) {
        WasalElkheer wasalElkheer = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        wasalElkheer.setStatus(newStatus);
        WasalElkheer updated = wasalElkheerRepository.save(wasalElkheer);
        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public List<WasalElkheerDto> getAllOrders() {
        return wasalElkheerRepository.findAll()
                .stream()
                .map(wasalElkheerMapper::toDto)
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

    private CreationWasalElkheerDto enrichCreationDto(CreationWasalElkheerDto creationDto) {
        if (creationDto.getUserId() != null) {
            userRepository.findById(creationDto.getUserId()).ifPresent(user ->
                    creationDto.setUserName(user.getName())
            );
        }
        if (creationDto.getDriverId() != null) {
            userRepository.findById(creationDto.getDriverId()).ifPresent(driver ->
                    creationDto.setDriverName(driver.getName())
            );
        }
        return creationDto;
    }
}