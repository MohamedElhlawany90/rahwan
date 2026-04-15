package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.charity.Charity;
import com.blueWhale.Rahwan.charity.CharityRepository;
import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.otp.OrderOtpService;
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
    private final OrderOtpService otpService;

    public CreationWasalElkheerDto createWasalElkheer(WasalElkheerForm form, UUID userId) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive()) throw new BusinessException("User account is not active");

        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + form.getCharityId()));
        if (!charity.isActive()) throw new BusinessException("Charity is not active");

        WasalElkheer wasalElkheer = wasalElkheerMapper.toEntity(form);
        wasalElkheer.setUserId(userId);

        // ✅ FIX: Use form helper for safe LocalTime parsing
        wasalElkheer.setCollectionTime(form.getParsedCollectionTime());

        savePhoto(form, wasalElkheer);
        wasalElkheer.setCreationStatus(CreationStatus.CREATED);

        WasalElkheer saved = wasalElkheerRepository.save(wasalElkheer);
        return enrichCreationDto(wasalElkheerMapper.toCreationWasalDto(saved));
    }

    public WasalElkheerDto confirmOrder(Long orderId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getCreationStatus() != CreationStatus.CREATED)
            throw new BusinessException("Order must be in CREATED status to confirm");

        // ✅ FIX: Generate pickup OTP here at confirm time (consistent with regular Order flow)
        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);
        order.setStatus(WasalElkheerStatus.PENDING);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        userRepository.findById(order.getUserId()).ifPresent(user -> {
            whatsAppService.sendWasalElkheerConfirmation(user.getPhone(), updated.getId());
            // Send pickup OTP to the donor
            whatsAppService.sendPickupOtpToSender(user.getPhone(), user.getName(), pickupOtp);
        });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public CreationWasalElkheerDto updateOrder(Long orderId, WasalElkheerForm form, UUID userId) throws IOException {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!order.getUserId().equals(userId) && !currentUser.isAdmin())
            throw new BusinessException("You are not allowed to update this order");

        if (order.getStatus() != null &&
                (order.getStatus() == WasalElkheerStatus.ACCEPTED
                        || order.getStatus() == WasalElkheerStatus.IN_PROGRESS
                        || order.getStatus() == WasalElkheerStatus.IN_THE_WAY
                        || order.getStatus() == WasalElkheerStatus.DELIVERED
                        || order.getStatus() == WasalElkheerStatus.RETURN))
            throw new BusinessException("Order cannot be updated in current status");

        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + form.getCharityId()));
        if (!charity.isActive()) throw new BusinessException("Charity is not active");

        order.setUserLatitude(form.getUserLatitude());
        order.setUserLongitude(form.getUserLongitude());
        order.setCharityId(form.getCharityId());
        order.setAddress(form.getAddress());
        order.setAdditionalNotes(form.getAdditionalNotes());
        order.setOrderType(form.getOrderType());
        order.setCollectionDate(form.getCollectionDate());
        // ✅ FIX: Use form helper for safe LocalTime parsing
        order.setCollectionTime(form.getParsedCollectionTime());
        order.setAnyTime(form.isAnyTime());
        order.setAllowInspection(form.isAllowInspection());
        order.setShippingPaidByReceiver(form.isShippingPaidByReceiver());

        savePhoto(form, order);

        WasalElkheer saved = wasalElkheerRepository.save(order);
        return enrichCreationDto(wasalElkheerMapper.toCreationWasalDto(saved));
    }

    public WasalElkheerDto driverConfirmOrder(Long orderId, UUID driverId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isDriver()) throw new BusinessException("Only drivers can accept orders");
        if (!driver.isActive()) throw new BusinessException("Driver account is not active");

        if (order.getStatus() != WasalElkheerStatus.PENDING)
            throw new BusinessException("Order cannot be accepted in current status");
        if (order.getDriverId() != null)
            throw new BusinessException("Order already accepted by another driver");

        order.setDriverId(driverId);
        order.setStatus(WasalElkheerStatus.ACCEPTED);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // ✅ FIX: The old code passed the OTP string as the "trackingNumber" argument in
        // sendDriverAcceptedNotification(), which would have sent the OTP to the wrong person
        // in the wrong message template, leaking it. Now we correctly notify the user that
        // their order was accepted (no OTP in this message — OTP was already sent at confirmOrder).
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDriverAcceptedNotification(
                        user.getPhone(),
                        driver.getName(),
                        String.valueOf(order.getId()) // order ID as reference, not the OTP
                ));

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto confirmPickup(Long orderId, String otp) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.ACCEPTED)
            throw new BusinessException("Order must be in ACCEPTED status");

        if (!otp.equals(order.getOtpForPickup()))
            throw new BusinessException("Invalid OTP");

        // ✅ FIX: Was using generatePickupOtp() for the delivery OTP — now correctly uses generateDeliveryOtp()
        String deliveryOtp = otpService.generateDeliveryOtp();
        order.setOtpForDelivery(deliveryOtp);

        order.setPickupConfirmed(true);
        order.setStatus(WasalElkheerStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // Send delivery OTP to the charity
        charityRepository.findById(order.getCharityId())
                .ifPresent(charity -> {
                    if (charity.getPhone() != null && !charity.getPhone().isEmpty()) {
                        whatsAppService.sendDeliveryOtpToRecipient(
                                charity.getPhone(), charity.getNameEn(), deliveryOtp
                        );
                    }
                });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto updateToInTheWay(Long orderId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.IN_PROGRESS)
            throw new BusinessException("Order must be in IN_PROGRESS status");

        order.setStatus(WasalElkheerStatus.IN_THE_WAY);
        return enrichDto(wasalElkheerMapper.toDto(wasalElkheerRepository.save(order)));
    }

    public WasalElkheerDto confirmDelivery(Long orderId, String otp) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.IN_PROGRESS && order.getStatus() != WasalElkheerStatus.IN_THE_WAY)
            throw new BusinessException("Order must be IN_PROGRESS or IN_THE_WAY");

        if (!otp.equals(order.getOtpForDelivery()))
            throw new BusinessException("Invalid delivery OTP");

        order.setDeliveryConfirmed(true);
        order.setStatus(WasalElkheerStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        userRepository.findById(order.getUserId()).ifPresent(user -> {
            String message = String.format(
                    "✅ Donation Delivered Successfully!\n\n📦 Order ID: %d\n🙏 Thank you for your generous donation!\n\nMay God reward you for your kindness.",
                    order.getId());
            whatsAppService.send(user.getPhone(), message);
        });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto returnOrder(Long orderId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed())
            throw new BusinessException("Cannot return order that wasn't picked up");

        order.setStatus(WasalElkheerStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        userRepository.findById(order.getUserId()).ifPresent(user -> {
            String message = String.format(
                    "🔄 Donation Returned\n\n📦 Order ID: %d\nThe donation items have been returned to you.\n\nPlease contact support if you have questions.",
                    order.getId());
            whatsAppService.send(user.getPhone(), message);
        });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto cancelOrderByDriver(Long orderId, UUID driverId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isDriver()) throw new BusinessException("Only drivers can cancel orders");

        // ✅ FIX: A driver who accepted the order (ACCEPTED status) should also be able to cancel,
        // not just pending orders where driverId is null. Old code blocked this incorrectly.
        if (order.getStatus() != WasalElkheerStatus.PENDING && order.getStatus() != WasalElkheerStatus.ACCEPTED)
            throw new BusinessException("Order cannot be cancelled. Current status: " + order.getStatus());

        if (order.getStatus() == WasalElkheerStatus.ACCEPTED && !driverId.equals(order.getDriverId()))
            throw new BusinessException("Only the assigned driver can cancel this order");

        order.setStatus(WasalElkheerStatus.CANCELLED);
        order.setDriverId(null); // ✅ release the order so another driver can pick it up later if needed

        WasalElkheer updated = wasalElkheerRepository.save(order);

        userRepository.findById(order.getUserId()).ifPresent(user -> {
            String message = String.format(
                    "❌ Donation Order Cancelled\n\n📦 Order ID: %d\n📝 Reason: Driver cancelled the order\n\nYour order may be reassigned.",
                    order.getId());
            whatsAppService.send(user.getPhone(), message);
        });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto cancelOrderByUser(Long orderId, UUID userId, String cancellationReason) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!order.getUserId().equals(userId) && !user.isAdmin())
            throw new BusinessException("You are not authorized to cancel this order");

        if (order.getStatus() == WasalElkheerStatus.IN_THE_WAY
                || order.getStatus() == WasalElkheerStatus.DELIVERED
                || order.getStatus() == WasalElkheerStatus.RETURN)
            throw new BusinessException("Order cannot be cancelled at this stage: " + order.getStatus());

        if (order.getStatus() == WasalElkheerStatus.CANCELLED)
            throw new BusinessException("Order is already cancelled");

        order.setStatus(WasalElkheerStatus.CANCELLED);
        WasalElkheer updated = wasalElkheerRepository.save(order);

        if (order.getDriverId() != null) {
            userRepository.findById(order.getDriverId()).ifPresent(driver -> {
                String message = String.format(
                        "❌ Donation Order Cancelled by User\n\n📦 Order ID: %d\n📝 Reason: %s",
                        order.getId(),
                        cancellationReason != null ? cancellationReason : "User cancelled the order");
                whatsAppService.send(driver.getPhone(), message);
            });
        }

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto updateOrderStatus(Long orderId, WasalElkheerStatus newStatus) {
        WasalElkheer wasalElkheer = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        wasalElkheer.setStatus(newStatus);
        return enrichDto(wasalElkheerMapper.toDto(wasalElkheerRepository.save(wasalElkheer)));
    }

    public List<WasalElkheerDto> getUserOrders(UUID userId) {
        return wasalElkheerRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(wasalElkheerMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getCharityOrders(Long charityId) {
        return wasalElkheerRepository.findByCharityIdOrderByCreatedAtDesc(charityId)
                .stream().map(wasalElkheerMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getOrdersByStatus(WasalElkheerStatus status) {
        return wasalElkheerRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(wasalElkheerMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public WasalElkheerDto getOrderById(Long orderId) {
        WasalElkheer wasalElkheer = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return enrichDto(wasalElkheerMapper.toDto(wasalElkheer));
    }

    public List<WasalElkheerDto> getAllOrders() {
        return wasalElkheerRepository.findAll()
                .stream().map(wasalElkheerMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getAvailableOrders() {
        return wasalElkheerRepository.findByStatusOrderByCreatedAtDesc(WasalElkheerStatus.PENDING)
                .stream().map(wasalElkheerMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    private WasalElkheerDto enrichDto(WasalElkheerDto dto) {
        if (dto.getUserId() != null)
            userRepository.findById(dto.getUserId()).ifPresent(u -> dto.setUserName(u.getName()));
        if (dto.getCharityId() != null)
            charityRepository.findById(dto.getCharityId()).ifPresent(c -> {
                dto.setCharityNameAr(c.getNameAr());
                dto.setCharityNameEn(c.getNameEn());
            });
        return dto;
    }

    private CreationWasalElkheerDto enrichCreationDto(CreationWasalElkheerDto dto) {
        if (dto.getUserId() != null)
            userRepository.findById(dto.getUserId()).ifPresent(u -> dto.setUserName(u.getName()));
        return dto;
    }

    /**
     * ✅ Extracted duplicate image-save logic into a private helper.
     * Follows DRY principle — was copy-pasted between createWasalElkheer and updateOrder.
     */
    private void savePhoto(WasalElkheerForm form, WasalElkheer order) throws IOException {
        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {
            Path uploadDir = Paths.get(UPLOADED_FOLDER);
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            byte[] bytes = ImageUtility.compressImage(form.getPhoto().getBytes());
            String fileName = new Date().getTime() + "A-A" + form.getPhoto().getOriginalFilename();
            Path path = uploadDir.resolve(fileName);
            Files.write(path, bytes);

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);

            order.setPhoto(fileName);
        }
    }
}