package com.blueWhale.Rahwan.orderorg;

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
public class OrderOrgService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final OrderOrgRepository orderOrgRepository;
    private final OrderOrgMapper orderOrgMapper;
    private final UserRepository userRepository;
    private final CharityRepository charityRepository;

    public OrderOrgDto createOrderOrg(OrderOrgForm form, UUID userId) throws IOException {

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

        OrderOrg orderOrg = orderOrgMapper.toEntity(form);
        orderOrg.setUserId(userId);

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
            orderOrg.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }
        orderOrg.setStatus(OrderOrgStatus.PENDING);

        OrderOrg saved = orderOrgRepository.save(orderOrg);
        return enrichDto(orderOrgMapper.toDto(saved));
    }

    public List<OrderOrgDto> getUserOrders(UUID userId) {
        return orderOrgRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderOrgMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<OrderOrgDto> getCharityOrders(Long charityId) {
        return orderOrgRepository.findByCharityIdOrderByCreatedAtDesc(charityId)
                .stream()
                .map(orderOrgMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<OrderOrgDto> getOrdersByStatus(OrderOrgStatus status) {
        return orderOrgRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(orderOrgMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public OrderOrgDto getOrderById(Long orderId) {
        OrderOrg orderOrg = orderOrgRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return enrichDto(orderOrgMapper.toDto(orderOrg));
    }

    public OrderOrgDto updateOrderStatus(Long orderId, OrderOrgStatus newStatus) {
        OrderOrg orderOrg = orderOrgRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        orderOrg.setStatus(newStatus);
        OrderOrg updated = orderOrgRepository.save(orderOrg);
        return enrichDto(orderOrgMapper.toDto(updated));
    }

    public List<OrderOrgDto> getAllOrders() {
        return orderOrgRepository.findAll()
                .stream()
                .map(orderOrgMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    private OrderOrgDto enrichDto(OrderOrgDto dto) {
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