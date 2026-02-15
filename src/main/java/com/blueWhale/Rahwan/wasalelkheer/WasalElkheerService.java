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
import java.time.LocalTime;
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

    /**
     * 1. إنشاء طلب Wasal Elkheer
     */
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

        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getPhoto() != null) {
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

    /**
     * 2. تأكيد الطلب
     */
    public WasalElkheerDto confirmOrder(Long orderId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getCreationStatus() != CreationStatus.CREATED) {
            throw new RuntimeException("Order must be in Created status to confirm");
        }
        order.setStatus(WasalElkheerStatus.PENDING);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إرسال تأكيد طلب التبرع
        userRepository.findById(order.getUserId()).ifPresent(user ->
                whatsAppService.sendWasalElkheerConfirmation(
                        user.getPhone(),
                        updated.getId()
                )
        );

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 3. تعديل الطلب
     * Authorization: User (owner) or Admin
     */
    public CreationWasalElkheerDto updateOrder(Long orderId, WasalElkheerForm form, UUID userId) throws IOException {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // جلب المستخدم للتحقق من الصلاحيات
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // السماح بالتعديل إذا كان المستخدم هو صاحب الطلب أو أدمن
        if (!order.getUserId().equals(userId) && !currentUser.isAdmin()) {
            throw new BusinessException("You are not allowed to update this order");
        }

        // الأوردر مينفعش يتعدل لو اتحرك خلاص
        if (order.getStatus() != null &&
                (order.getStatus() == WasalElkheerStatus.ACCEPTED
                        || order.getStatus() == WasalElkheerStatus.IN_PROGRESS
                        || order.getStatus() == WasalElkheerStatus.IN_THE_WAY
                        || order.getStatus() == WasalElkheerStatus.DELIVERED
                        || order.getStatus() == WasalElkheerStatus.RETURN)) {
            throw new RuntimeException("Order cannot be updated in current status");
        }

        // التحقق من الجمعية
        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found with id: " + form.getCharityId()));

        if (!charity.isActive()) {
            throw new RuntimeException("Charity is not active");
        }

        // تحديث البيانات الأساسية
        order.setUserLatitude(form.getUserLatitude());
        order.setUserLongitude(form.getUserLongitude());
        order.setCharityId(form.getCharityId());
        order.setAddress(form.getAddress());
        order.setAdditionalNotes(form.getAdditionalNotes());
        order.setOrderType(form.getOrderType());
        order.setCollectionDate(form.getCollectionDate());
        order.setCollectionTime(LocalTime.parse(form.getCollectionTime()));
        order.setAnyTime(form.isAnyTime());
        order.setAllowInspection(form.isAllowInspection());
        order.setShippingPaidByReceiver(form.isShippingPaidByReceiver());

        // تحديث الصورة لو موجودة
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (form.getPhoto() != null && !form.getPhoto().isEmpty()) {

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

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

        WasalElkheer saved = wasalElkheerRepository.save(order);

        return enrichCreationDto(wasalElkheerMapper.toCreationWasalDto(saved));
    }

    /**
     * 4. السائق يقبل الطلب
     * Authorization: Driver only
     */
    public WasalElkheerDto driverConfirmOrder(Long orderId, UUID driverId) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        // التحقق من أن المستخدم هو سائق
        if (!driver.isDriver()) {
            throw new BusinessException("Only drivers can accept orders");
        }

        if (!driver.isActive()) {
            throw new RuntimeException("Driver account is not active");
        }

        if (order.getStatus() != WasalElkheerStatus.PENDING) {
            throw new RuntimeException("Order cannot be accepted in current status");
        }

        if (order.getDriverId() != null) {
            throw new RuntimeException("Order already accepted by another driver");
        }

        // توليد OTP للاستلام
        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);

        order.setDriverId(driverId);
        order.setStatus(WasalElkheerStatus.ACCEPTED);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إشعار المستخدم بقبول السائق + OTP
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDriverAcceptedNotification(
                        user.getPhone(),
                        driver.getName(),
                        pickupOtp
                ));

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 5. السائق يؤكد الاستلام بـ OTP
     * Authorization: Driver who accepted the order
     */
    public WasalElkheerDto confirmPickup(Long orderId, String otp) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.ACCEPTED) {
            throw new RuntimeException("Order must be in ACCEPTED status");
        }

        if (!otp.equals(order.getOtpForPickup())) {
            throw new RuntimeException("Invalid OTP");
        }

        // توليد OTP للتسليم (للجمعية)
        String deliveryOtp = otpService.generatePickupOtp();
        order.setOtpForDelivery(deliveryOtp);

        order.setPickupConfirmed(true);
        order.setStatus(WasalElkheerStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إرسال OTP للجمعية (إذا كان لها رقم)
        charityRepository.findById(order.getCharityId())
                .ifPresent(charity -> {
                    if (charity.getPhone() != null && !charity.getPhone().isEmpty()) {
                        // استخدام sendOtp للجمعية
                        whatsAppService.sendOtp(charity.getPhone(), deliveryOtp);
                    }
                });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 6. السائق يحدث الحالة إلى "في الطريق"
     * Authorization: Driver who accepted the order
     */
    public WasalElkheerDto updateToInTheWay(Long orderId) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.IN_PROGRESS) {
            throw new RuntimeException("Order must be in IN_PROGRESS status");
        }

        order.setStatus(WasalElkheerStatus.IN_THE_WAY);
        WasalElkheer updated = wasalElkheerRepository.save(order);

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 7. السائق يؤكد التسليم بـ OTP
     * Authorization: Driver who accepted the order
     */
    public WasalElkheerDto confirmDelivery(Long orderId, String otp) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.IN_PROGRESS && order.getStatus() != WasalElkheerStatus.IN_THE_WAY) {
            throw new RuntimeException("Order must be IN_PROGRESS or IN_THE_WAY");
        }

        if (!otp.equals(order.getOtpForDelivery())) {
            throw new RuntimeException("Invalid delivery OTP");
        }

        order.setDeliveryConfirmed(true);
        order.setStatus(WasalElkheerStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إشعار المستخدم بنجاح التبرع
        userRepository.findById(order.getUserId())
                .ifPresent(user -> {
                    // يمكننا استخدام send() مباشرة برسالة مخصصة
                    String message = String.format(
                            "✅ Donation Delivered Successfully!\n\n" +
                                    "📦 Order ID: %d\n" +
                                    "🙏 Thank you for your generous donation!\n\n" +
                                    "May God reward you for your kindness.",
                            order.getId()
                    );
                    whatsAppService.send(user.getPhone(), message);
                });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 8. السائق يرجع الطلب
     * Authorization: Driver who accepted the order
     */
    public WasalElkheerDto returnOrder(Long orderId) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Cannot return order that wasn't picked up");
        }

        order.setStatus(WasalElkheerStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إشعار المستخدم بإرجاع التبرع
        userRepository.findById(order.getUserId())
                .ifPresent(user -> {
                    String message = String.format(
                            "🔄 Donation Returned\n\n" +
                                    "📦 Order ID: %d\n" +
                                    "The donation items have been returned to you.\n\n" +
                                    "Please contact support if you have questions.",
                            order.getId()
                    );
                    whatsAppService.send(user.getPhone(), message);
                });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 9. السائق يلغي الطلب (قبل القبول فقط)
     * Authorization: Driver
     */
    public WasalElkheerDto cancelOrderByDriver(Long orderId, UUID driverId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isDriver()) {
            throw new BusinessException("Only drivers can cancel orders");
        }

        if (order.getStatus() != WasalElkheerStatus.PENDING) {
            throw new BusinessException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        if (order.getDriverId() != null) {
            throw new BusinessException("Order is already accepted by a driver");
        }

        order.setStatus(WasalElkheerStatus.CANCELLED);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إشعار المستخدم بإلغاء السائق
        userRepository.findById(order.getUserId())
                .ifPresent(user -> {
                    String message = String.format(
                            "❌ Donation Order Cancelled\n\n" +
                                    "📦 Order ID: %d\n" +
                                    "📝 Reason: Driver cancelled the order\n\n" +
                                    "Your order is now available for other drivers.",
                            order.getId()
                    );
                    whatsAppService.send(user.getPhone(), message);
                });

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * 10. المستخدم يلغي الطلب
     * Authorization: User (owner) or Admin
     */
    public WasalElkheerDto cancelOrderByUser(Long orderId, UUID userId, String cancellationReason) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // التحقق من الصلاحيات
        if (!order.getUserId().equals(userId) && !user.isAdmin()) {
            throw new BusinessException("You are not authorized to cancel this order");
        }

        if (order.getStatus() == WasalElkheerStatus.IN_THE_WAY ||
                order.getStatus() == WasalElkheerStatus.DELIVERED ||
                order.getStatus() == WasalElkheerStatus.RETURN) {
            throw new BusinessException("Order cannot be cancelled at this stage: " + order.getStatus());
        }

        if (order.getStatus() == WasalElkheerStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }

        // الحالة 1: الطلب في PENDING ولم يقبله سائق - إلغاء مجاني
        if (order.getStatus() == WasalElkheerStatus.PENDING && order.getDriverId() == null) {
            order.setStatus(WasalElkheerStatus.CANCELLED);

            WasalElkheer updated = wasalElkheerRepository.save(order);
            return enrichDto(wasalElkheerMapper.toDto(updated));
        }

        // الحالة 2: السائق قبل الطلب - إلغاء مع إشعار
        if (order.getDriverId() != null &&
                (order.getStatus() == WasalElkheerStatus.ACCEPTED || order.getStatus() == WasalElkheerStatus.IN_PROGRESS)) {

            order.setStatus(WasalElkheerStatus.CANCELLED);

            WasalElkheer updated = wasalElkheerRepository.save(order);

            // إشعار السائق
            userRepository.findById(order.getDriverId())
                    .ifPresent(driver -> {
                        String message = String.format(
                                "❌ Donation Order Cancelled by User\n\n" +
                                        "📦 Order ID: %d\n" +
                                        "📝 Reason: %s",
                                order.getId(),
                                cancellationReason != null ? cancellationReason : "User cancelled the order"
                        );
                        whatsAppService.send(driver.getPhone(), message);
                    });

            return enrichDto(wasalElkheerMapper.toDto(updated));
        }

        throw new BusinessException("Unable to cancel order in current state");
    }

    /**
     * 11. Admin: تحديث حالة الطلب
     * Authorization: Admin only
     */
    public WasalElkheerDto updateOrderStatus(Long orderId, WasalElkheerStatus newStatus) {
        WasalElkheer wasalElkheer = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        wasalElkheer.setStatus(newStatus);
        WasalElkheer updated = wasalElkheerRepository.save(wasalElkheer);
        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    // ==================== Query Methods ====================

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

    public List<WasalElkheerDto> getAllOrders() {
        return wasalElkheerRepository.findAll()
                .stream()
                .map(wasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<WasalElkheerDto> getAvailableOrders() {
        return wasalElkheerRepository.findByStatusOrderByCreatedAtDesc(WasalElkheerStatus.PENDING)
                .stream()
                .map(wasalElkheerMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

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
        return creationDto;
    }
}