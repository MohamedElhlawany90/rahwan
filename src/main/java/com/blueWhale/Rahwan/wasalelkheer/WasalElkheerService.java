package com.blueWhale.Rahwan.wasalelkheer;

import com.blueWhale.Rahwan.charity.Charity;
import com.blueWhale.Rahwan.charity.CharityRepository;
import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.order.CreationDto;
import com.blueWhale.Rahwan.order.Order;
import com.blueWhale.Rahwan.order.OrderDto;
import com.blueWhale.Rahwan.order.OrderStatus;
import com.blueWhale.Rahwan.otp.OrderOtpService;
import com.blueWhale.Rahwan.otp.OtpService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import com.blueWhale.Rahwan.util.ImageUtility;
import com.blueWhale.Rahwan.wallet.Wallet;
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
    private final OrderOtpService otpService ;


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

        userRepository.findById(order.getUserId()).ifPresent(user ->
                whatsAppService.sendWasalElkheerConfirmation(
                        user.getPhone(),
                        updated.getId()
                )
        );        return enrichDto(wasalElkheerMapper.toDto(updated));

    }
    public CreationWasalElkheerDto updateOrder(Long orderId, WasalElkheerForm form, UUID userId) throws IOException {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));


        // جلب المستخدم للتحقق من نوعه
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // السماح بالتعديل إذا كان المستخدم هو صاحب الطلب أو أدمن
        if (!order.getUserId().equals(userId) && !currentUser.getType().equals("admin")) {
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


    public WasalElkheerDto driverConfirmOrder(Long orderId, UUID driverId) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING status");
        }

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isActive()) {
            throw new RuntimeException("Driver account is not active");
        }



        // توليد OTP للتسليم
        String deliveryOtp = otpService.generatePickupOtp();
        order.setOtpForDelivery(deliveryOtp);

        // تحديث الطلب
        order.setDriverId(driverId);
        order.setStatus(WasalElkheerStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

//        // إرسال OTP للمستلم
//        otpService.sendDeliveryOtp(order.getRecipientPhone(), deliveryOtp);

        // إرسال تأكيد للـ User
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            whatsAppService.sendDriverAcceptedNotification(
                    user.getPhone(),
                    driver.getName(),
                    order.getOtpForPickup()
            );
        }

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto confirmPickup(Long orderId, String otpFromUser) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != WasalElkheerStatus.ACCEPTED) {
            throw new RuntimeException("Order must be in ACCEPTED status");
        }

        if (!order.getOtpForPickup().equals(otpFromUser)) {
            throw new RuntimeException("Invalid OTP for pickup");
        }

        order.setPickupConfirmed(true);
        order.setStatus(WasalElkheerStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);
        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

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

    public WasalElkheerDto confirmDelivery(Long orderId, String otpFromRecipient) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Pickup must be confirmed first");
        }

        if (order.getStatus() != WasalElkheerStatus.IN_PROGRESS
                && order.getStatus() != WasalElkheerStatus.IN_THE_WAY) {
            throw new RuntimeException("Order must be in progress");
        }

        if (!order.getOtpForDelivery().equals(otpFromRecipient)) {
            throw new RuntimeException("Invalid OTP for delivery");
        }

        order.setDeliveryConfirmed(true);
        order.setStatus(WasalElkheerStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);

        userRepository.findById(order.getUserId())
                .ifPresent(user ->
                        whatsAppService.sendDeliveryConfirmation(user.getPhone(), order.getId().toString())
                );

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    public WasalElkheerDto returnOrder(Long orderId) {

        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Cannot return order that wasn't picked up");
        }

        order.setStatus(WasalElkheerStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        WasalElkheer updated = wasalElkheerRepository.save(order);
        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * إلغاء الطلب من قبل السائق (قبل القبول فقط)
     */
    public WasalElkheerDto cancelOrderByDriver(Long orderId, UUID driverId) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق أن الطلب في حالة PENDING
        if (order.getStatus() != WasalElkheerStatus.PENDING) {
            throw new BusinessException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        // السائق يمكنه الإلغاء فقط إذا كان الطلب متاح للجميع (لم يقبله أحد)
        if (order.getDriverId() != null) {
            throw new BusinessException("Order is already accepted by a driver");
        }

        // تحديث الحالة
        order.setStatus(WasalElkheerStatus.CANCELLED);

        WasalElkheer updated = wasalElkheerRepository.save(order);

        // إشعار المستخدم
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendOrderCancellation(
                        user.getPhone(),
                        order.getId().toString(),
                        "Driver cancelled the donation order"
                ));

        return enrichDto(wasalElkheerMapper.toDto(updated));
    }

    /**
     * إلغاء الطلب من قبل المستخدم
     * - إذا كان الطلب في PENDING: إلغاء مجاني
     * - إذا كان الطلب في ACCEPTED أو IN_PROGRESS: يتم الإلغاء مع إشعار السائق
     */
    public WasalElkheerDto cancelOrderByUser(Long orderId, UUID userId, String cancellationReason) {
        WasalElkheer order = wasalElkheerRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق من ملكية الطلب
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You are not authorized to cancel this order");
        }

        // لا يمكن الإلغاء بعد استلام الطلب
        if (order.getStatus() == WasalElkheerStatus.IN_THE_WAY ||
                order.getStatus() == WasalElkheerStatus.DELIVERED ||
                order.getStatus() == WasalElkheerStatus.RETURN) {
            throw new BusinessException("Order cannot be cancelled at this stage: " + order.getStatus());
        }

        // إذا كان الطلب ملغي أصلاً
        if (order.getStatus() == WasalElkheerStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }

        // الحالة 1: الطلب في PENDING ولم يقبله سائق بعد - إلغاء مجاني
        if (order.getStatus() == WasalElkheerStatus.PENDING && order.getDriverId() == null) {
            order.setStatus(WasalElkheerStatus.CANCELLED);

            WasalElkheer updated = wasalElkheerRepository.save(order);
            return enrichDto(wasalElkheerMapper.toDto(updated));
        }

        // الحالة 2: السائق قبل الطلب (ACCEPTED أو IN_PROGRESS) - إلغاء مع إشعار
        if (order.getDriverId() != null &&
                (order.getStatus() == WasalElkheerStatus.ACCEPTED || order.getStatus() == WasalElkheerStatus.IN_PROGRESS)) {

            order.setStatus(WasalElkheerStatus.CANCELLED);

            WasalElkheer updated = wasalElkheerRepository.save(order);

            // إشعار السائق
            userRepository.findById(order.getDriverId())
                    .ifPresent(driver -> whatsAppService.sendOrderCancellation(
                            driver.getPhone(),
                            order.getId().toString(),
                            "User cancelled the donation order" + (cancellationReason != null ? ": " + cancellationReason : "")
                    ));

            return enrichDto(wasalElkheerMapper.toDto(updated));
        }

        // حالة افتراضية (لا ينبغي الوصول إليها)
        throw new BusinessException("Unable to cancel order in current state");
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

    public List<WasalElkheerDto> getAvailableOrders() {
        return wasalElkheerRepository.findByStatusOrderByCreatedAtDesc(WasalElkheerStatus.PENDING)
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
//        if (creationDto.getDriverId() != null) {
//            userRepository.findById(creationDto.getDriverId()).ifPresent(driver ->
//                    creationDto.setDriverName(driver.getName())
//            );
//        }
        return creationDto;
    }
}