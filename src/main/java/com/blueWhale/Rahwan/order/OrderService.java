package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.order.service.CostCalculationService;
import com.blueWhale.Rahwan.order.service.PricingDetails;
import com.blueWhale.Rahwan.otp.OrderOtpService;
import com.blueWhale.Rahwan.commission.CommissionSettings;
import com.blueWhale.Rahwan.commission.CommissionSettingsService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import com.blueWhale.Rahwan.util.ImageUtility;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletService;
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
public class OrderService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final CostCalculationService costCalculationService;
    private final WalletService walletService;
    private final OrderOtpService otpService;
    private final WhatsAppService whatsAppService;
    private final CommissionSettingsService commissionSettingsService;

    /**
     * 1. إنشاء طلب جديد من User
     * Status: CREATED (بدون أي تجميد)
     */
    public CreationDto createOrder(OrderForm orderForm, UUID userId) throws IOException {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BusinessException("User account is not active");
        }

        PricingDetails cost = costCalculationService.calculateCost(
                orderForm.getPickupLatitude(),
                orderForm.getPickupLongitude(),
                orderForm.getRecipientLatitude(),
                orderForm.getRecipientLongitude(),
                orderForm.getInsuranceValue()
        );

        // جلب نسبة العمولة
        CommissionSettings commissionSettings = commissionSettingsService.getActiveSettings();
        double commissionRate = commissionSettings.getCommissionRate();

        // حساب العمولة من insurance value (لأن دي اللي هتتحول للتطبيق)
        double totalCost = cost.getTotalCost();
        double appCommission = round((orderForm.getInsuranceValue() * commissionRate) / 100.0);

        Order order = orderMapper.toEntity(orderForm);
        order.setUserId(userId);
        order.setDeliveryCost(totalCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setTrackingNumber(generateTrackingNumber());
        order.setCreationStatus(CreationStatus.CREATED);

        // رفع الصورة
        Path uploadDir = Paths.get(UPLOADED_FOLDER);
        if (orderForm.getPhoto() != null) {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(orderForm.getPhoto().getBytes());
            Path path = Paths.get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + orderForm.getPhoto().getOriginalFilename());
            String url = Files.write(path, bytes).toUri().getPath();
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);
            order.setPhoto(url.substring(url.lastIndexOf("/") + 1));
        }

        Order saved = orderRepository.save(order);

        return enrichCreationDto(orderMapper.toCreationDto(saved));
    }

    /**
     * 2. تأكيد الطلب
     * Status: CREATED → PENDING
     * تجميد: ضعف تمن التوصيل من User
     */
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getCreationStatus() != CreationStatus.CREATED) {
            throw new RuntimeException("Order must be in CREATED status to confirm");
        }

        // توليد OTP للاستلام
        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);

        // تجميد ضعف تمن التوصيل من User
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        double userFreezeAmount = order.getDeliveryCost() * 2;

        walletService.freezeAmount(userWallet, userFreezeAmount);

        // تحديث الحالة
        order.setStatus(OrderStatus.PENDING);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إرسال إشعارات
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            whatsAppService.sendOtp(user.getPhone(), pickupOtp);
            whatsAppService.sendOrderConfirmation(
                    user.getPhone(),
                    order.getTrackingNumber(),
                    order.getDeliveryCost()
            );
        }

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 3. تحديث الطلب
     * Authorization: User (owner) or Admin
     * يمكن التعديل فقط في حالة CREATED أو PENDING (قبل قبول السائق)
     */
    public CreationDto updateOrder(Long orderId, OrderForm orderForm, UUID userId) throws IOException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!order.getUserId().equals(userId) && !currentUser.isAdmin()) {
            throw new BusinessException("You are not allowed to update this order");
        }

        // يمكن التعديل فقط قبل قبول السائق
        if (order.getStatus() != null && order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Order cannot be updated in current status");
        }

        // إعادة حساب التكلفة والعمولة
        PricingDetails cost = costCalculationService.calculateCost(
                orderForm.getPickupLatitude(),
                orderForm.getPickupLongitude(),
                orderForm.getRecipientLatitude(),
                orderForm.getRecipientLongitude(),
                orderForm.getInsuranceValue()
        );

        CommissionSettings commissionSettings = commissionSettingsService.getActiveSettings();
        double commissionRate = commissionSettings.getCommissionRate();
        double totalCost = cost.getTotalCost();
        double appCommission = round((orderForm.getInsuranceValue() * commissionRate) / 100.0);

        // إذا كان الطلب في PENDING، نحتاج نعدل المبلغ المجمد
        if (order.getStatus() == OrderStatus.PENDING) {
            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());

            // فك التجميد القديم
            double oldFreezeAmount = order.getDeliveryCost() * 2;
            walletService.unfreezeAmount(userWallet, oldFreezeAmount);

            // تجميد جديد بالسعر الجديد
            double newFreezeAmount = totalCost * 2;
            walletService.freezeAmount(userWallet, newFreezeAmount);
        }

        // تحديث البيانات
        order.setPickupLatitude(orderForm.getPickupLatitude());
        order.setPickupLongitude(orderForm.getPickupLongitude());
        order.setPickupAddress(orderForm.getPickupAddress());
        order.setRecipientLatitude(orderForm.getRecipientLatitude());
        order.setRecipientLongitude(orderForm.getRecipientLongitude());
        order.setRecipientAddress(orderForm.getRecipientAddress());
        order.setRecipientName(orderForm.getRecipientName());
        order.setRecipientPhone(orderForm.getRecipientPhone());
        order.setOrderType(orderForm.getOrderType());
        order.setInsuranceValue(orderForm.getInsuranceValue());
        order.setDeliveryCost(totalCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setAdditionalNotes(orderForm.getAdditionalNotes());
        order.setCollectionDate(orderForm.getCollectionDate());
        order.setCollectionTime(orderForm.getCollectionTime());
        order.setAnyTime(orderForm.getAnyTime());
        order.setAllowInspection(orderForm.getAllowInspection());
        order.setReceiverPaysShipping(orderForm.getReceiverPaysShipping());

        // تحديث الصورة
        Path uploadDir = Paths.get(UPLOADED_FOLDER);
        if (orderForm.getPhoto() != null && !orderForm.getPhoto().isEmpty()) {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(orderForm.getPhoto().getBytes());
            String fileName = new Date().getTime() + "A-A" + orderForm.getPhoto().getOriginalFilename();
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

        Order saved = orderRepository.save(order);
        return enrichCreationDto(orderMapper.toCreationDto(saved));
    }

    /**
     * 4. السائق يقبل الطلب
     * Status: PENDING → ACCEPTED
     * تجميد: بدون تجميد (التجميد يحصل عند الاستلام)
     * شرط: السائق لازم يكون عنده رصيد > insurance value للتجميد لاحقاً
     */
    public OrderDto driverConfirmOrder(Long orderId, UUID driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isDriver()) {
            throw new BusinessException("Only drivers can accept orders");
        }

        if (!driver.isActive()) {
            throw new BusinessException("Driver account is not active");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Order cannot be accepted in current status");
        }

        if (order.getDriverId() != null) {
            throw new BusinessException("Order already accepted by another driver");
        }

        // التحقق من رصيد السائق (لازم يكون عنده رصيد للتجميد لاحقاً)
        Wallet driverWallet = walletService.getWalletByUserId(driverId);
        if (driverWallet.getWalletBalance() < order.getInsuranceValue()) {
            throw new BusinessException(
                    "Insufficient balance. You need at least " + order.getInsuranceValue() +
                            " EGP to accept this order. Current balance: " + driverWallet.getWalletBalance() + " EGP"
            );
        }

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إشعار المستخدم
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDriverAcceptedNotification(
                        user.getPhone(),
                        driver.getName(),
                        order.getOtpForPickup()
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 5. السائق يؤكد الاستلام بـ OTP
     * Status: ACCEPTED → IN_PROGRESS
     * تجميد: insurance value من Driver
     * Authorization: Driver who accepted the order
     */
    public OrderDto confirmPickup(Long orderId, UUID driverId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق من أن السائق هو نفسه اللي قبل الطلب
        if (!order.getDriverId().equals(driverId)) {
            throw new BusinessException("Only the driver who accepted this order can confirm pickup");
        }

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new BusinessException("Order must be in ACCEPTED status");
        }

        if (!otp.equals(order.getOtpForPickup())) {
            throw new BusinessException("Invalid OTP");
        }

        // توليد OTP للتسليم
        String deliveryOtp = otpService.generatePickupOtp();
        order.setOtpForDelivery(deliveryOtp);

        // تجميد insurance value من Driver
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());
        walletService.freezeAmount(driverWallet, order.getInsuranceValue());

        // تحديث الحالة
        order.setPickupConfirmed(true);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إرسال OTP للمستلم
        whatsAppService.sendDeliveryOtpToRecipient(
                order.getRecipientPhone(),
                order.getRecipientName(),
                deliveryOtp
        );

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 6. السائق يؤكد التسليم بـ OTP
     * Status: IN_PROGRESS/IN_THE_WAY → DELIVERED
     *
     * العمليات المالية:
     * 1. فك تجميد ضعف الشحن من User
     * 2. فك تجميد insurance value من Driver
     * 3. تحويل (insurance value - commission) من Driver للUser
     * 4. commission يروح للتطبيق (نحتفظ بيه في مكان معين)
     * 5. Driver بياخد deliveryCost كاش من المستلم
     *
     * Authorization: Driver who accepted the order
     */
    public OrderDto confirmDelivery(Long orderId, UUID driverId, String otp) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق من أن السائق هو نفسه اللي قبل الطلب
        if (!order.getDriverId().equals(driverId)) {
            throw new BusinessException("Only the driver who accepted this order can confirm delivery");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.IN_THE_WAY) {
            throw new BusinessException("Order must be IN_PROGRESS or IN_THE_WAY");
        }

        if (!otp.equals(order.getOtpForDelivery())) {
            throw new BusinessException("Invalid delivery OTP");
        }

        order.setDeliveryConfirmed(true);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        // العمليات المالية
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        // 1. فك تجميد ضعف الشحن من User
        double userFrozenAmount = order.getDeliveryCost() * 2;
        walletService.unfreezeAmount(userWallet, userFrozenAmount);

        // 2. فك تجميد insurance value من Driver
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        // 3. تحويل (insurance value - commission) من Driver للUser
        //    هنا السائق بيدفع تمن الحاجة اللي استلمها للمستخدم
        //    ويخصم منه commission التطبيق
        double userReceives = order.getInsuranceValue() - order.getAppCommission();

        if (driverWallet.getWalletBalance() < order.getInsuranceValue()) {
            throw new BusinessException("Driver has insufficient balance");
        }

        // خصم insurance value من السائق
        driverWallet.setWalletBalance(driverWallet.getWalletBalance() - order.getInsuranceValue());

        // إضافة (insurance value - commission) للمستخدم
        userWallet.setWalletBalance(userWallet.getWalletBalance() + userReceives);

        // 4. الـ commission (appCommission) يروح للتطبيق
        //    هنا ممكن نعملها transfer لمحفظة admin أو نسيبها كـ revenue
        //    مؤقتاً: الـ commission بيضيع (لأننا خصمناه من insurance value)
        //    لو عايزين نحفظه، نعمل:
        //    - إما محفظة للـ app
        //    - أو جدول transactions يسجل الـ revenue

        walletService.save(userWallet);
        walletService.save(driverWallet);

        Order updated = orderRepository.save(order);

        // إشعارات
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDeliveryConfirmation(
                        user.getPhone(),
                        order.getTrackingNumber()
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 7. السائق يرجع الطلب
     * Status: IN_PROGRESS/IN_THE_WAY → RETURN
     *
     * العمليات المالية:
     * 1. فك تجميد ضعف الشحن من User
     * 2. فك تجميد insurance value من Driver
     * 3. بدون أي تحويلات (السائق رجع الحاجة)
     *
     * Authorization: Driver who accepted the order
     */
    public OrderDto returnOrder(Long orderId, UUID driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق من أن السائق هو نفسه اللي قبل الطلب
        if (!order.getDriverId().equals(driverId)) {
            throw new BusinessException("Only the driver who accepted this order can return it");
        }

        if (!order.isPickupConfirmed()) {
            throw new BusinessException("Cannot return order that wasn't picked up");
        }

        order.setStatus(OrderStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        // فك التجميد فقط
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        Order updated = orderRepository.save(order);

        // إشعار المستخدم
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendOrderCancellation(
                        user.getPhone(),
                        order.getTrackingNumber(),
                        "Order returned to sender"
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 8. السائق يلغي الطلب (قبل القبول فقط)
     * Status: PENDING → CANCELLED
     *
     * العمليات المالية:
     * 1. فك تجميد ضعف الشحن من User (لأن الطلب اتلغى)
     */
    public OrderDto cancelOrderByDriver(Long orderId, UUID driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        if (!driver.isDriver()) {
            throw new BusinessException("Only drivers can cancel orders");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Order cannot be cancelled. Current status: " + order.getStatus());
        }

        if (order.getDriverId() != null) {
            throw new BusinessException("Order is already accepted by a driver");
        }

        // فك تجميد من User
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 9. المستخدم يلغي الطلب
     * Authorization: User (owner) or Admin
     *
     * السيناريوهات:
     * 1. PENDING + لم يقبله سائق: إلغاء مجاني + فك التجميد
     * 2. ACCEPTED/IN_PROGRESS: يدفع deliveryCost للسائق كتعويض + فك التجميد
     */
    public OrderDto cancelOrderByUser(Long orderId, UUID userId, String cancellationReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!order.getUserId().equals(userId) && !user.isAdmin()) {
            throw new BusinessException("You are not authorized to cancel this order");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.RETURN) {
            throw new BusinessException("Order cannot be cancelled at this stage: " + order.getStatus());
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Order is already cancelled");
        }

        // السيناريو 1: PENDING ولم يقبله سائق - إلغاء مجاني
        if (order.getStatus() == OrderStatus.PENDING && order.getDriverId() == null) {
            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());

            // فك تجميد ضعف الشحن
            walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(cancellationReason);

            Order updated = orderRepository.save(order);

            whatsAppService.sendOrderCancellation(
                    user.getPhone(),
                    order.getTrackingNumber(),
                    cancellationReason != null ? cancellationReason : "Cancelled by user"
            );

            return enrichDto(orderMapper.toDto(updated));
        }

        // السيناريو 2: السائق قبل الطلب - دفع تعويض
        if (order.getDriverId() != null &&
                (order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.IN_PROGRESS)) {

            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
            Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

            // فك التجميد
            walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

            // إذا كان الطلب في IN_PROGRESS، نفك تجميد السائق كمان
            if (order.getStatus() == OrderStatus.IN_PROGRESS) {
                walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());
            }

            // دفع التعويض (deliveryCost) للسائق
            if (userWallet.getWalletBalance() < order.getDeliveryCost()) {
                throw new BusinessException("Insufficient balance to cancel order. Required: " +
                        order.getDeliveryCost() + ", Available: " + userWallet.getWalletBalance());
            }

            userWallet.setWalletBalance(userWallet.getWalletBalance() - order.getDeliveryCost());
            driverWallet.setWalletBalance(driverWallet.getWalletBalance() + order.getDeliveryCost());

            walletService.save(userWallet);
            walletService.save(driverWallet);

            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(cancellationReason != null ? cancellationReason : "Cancelled by user after driver acceptance");

            Order updated = orderRepository.save(order);

            // إشعار السائق
            userRepository.findById(order.getDriverId())
                    .ifPresent(driver -> whatsAppService.sendOrderCancellation(
                            driver.getPhone(),
                            order.getTrackingNumber(),
                            "User cancelled the order. You received " + order.getDeliveryCost() + " EGP as compensation"
                    ));

            return enrichDto(orderMapper.toDto(updated));
        }

        throw new BusinessException("Unable to cancel order in current state");
    }

    /**
     * 10. السائق يحدث الحالة إلى "في الطريق"
     * Status: IN_PROGRESS → IN_THE_WAY
     * Authorization: Driver who accepted the order
     */
    public OrderDto updateToInTheWay(Long orderId, UUID driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // التحقق من أن السائق هو نفسه اللي قبل الطلب
        if (!order.getDriverId().equals(driverId)) {
            throw new BusinessException("Only the driver who accepted this order can update status");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new BusinessException("Order must be in IN_PROGRESS status");
        }

        order.setStatus(OrderStatus.IN_THE_WAY);
        Order updated = orderRepository.save(order);

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 11. Admin: تغيير حالة الطلب
     * Authorization: Admin only
     */
    public OrderDto changeOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }

    // ==================== Query Methods ====================

    public List<OrderDto> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getDriverOrders(UUID driverId) {
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getAvailableOrders() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public List<OrderDto> getOrdersByUserAndStatus(UUID userId, OrderStatus status) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(order -> order.getStatus() == status)
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public OrderDto getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return enrichDto(orderMapper.toDto(order));
    }

    public OrderDto getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber));
        return enrichDto(orderMapper.toDto(order));
    }

    public OrderStatusCounts getOrdersCountsByUser(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long pending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long accepted = orders.stream().filter(o -> o.getStatus() == OrderStatus.ACCEPTED).count();
        long inProgress = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
        long inTheWay = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_THE_WAY).count();
        long returned = orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURN).count();
        long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();
        long allOrders = orders.size();
        long allActiveOrders = pending + accepted + inProgress + inTheWay;
        return new OrderStatusCounts(allOrders, allActiveOrders);
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    public OrderStatisticsDto getOrderStatistics(UUID userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        long pending = orders.stream().filter(order -> order.getStatus() == OrderStatus.PENDING).count();
        long accepted = orders.stream().filter(order -> order.getStatus() == OrderStatus.ACCEPTED).count();
        long inProgress = orders.stream().filter(order -> order.getStatus() == OrderStatus.IN_PROGRESS).count();
        long inTheWay = orders.stream().filter(order -> order.getStatus() == OrderStatus.IN_THE_WAY).count();
        long returned = orders.stream().filter(order -> order.getStatus() == OrderStatus.RETURN).count();
        long delivered = orders.stream().filter(order -> order.getStatus() == OrderStatus.DELIVERED).count();
        long cancelled = orders.stream().filter(order -> order.getStatus() == OrderStatus.CANCELLED).count();

        OrderStatisticsDto statisticsDto = new OrderStatisticsDto();
        statisticsDto.setTotalOrders(orders.size());
        statisticsDto.setPending(pending);
        statisticsDto.setAccepted(accepted);
        statisticsDto.setInProgress(inProgress);
        statisticsDto.setInTheWay(inTheWay);
        statisticsDto.setInDelivery(returned);
        statisticsDto.setDelivered(delivered);
        statisticsDto.setCancelled(cancelled);
        return statisticsDto;
    }

    // ==================== Helper Methods ====================

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String generateTrackingNumber() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private OrderDto enrichDto(OrderDto orderDto) {
        if (orderDto.getUserId() != null) {
            userRepository.findById(orderDto.getUserId()).ifPresent(user ->
                    orderDto.setUserName(user.getName())
            );
        }
        if (orderDto.getDriverId() != null) {
            userRepository.findById(orderDto.getDriverId()).ifPresent(driver ->
                    orderDto.setDriverName(driver.getName())
            );
        }
        return orderDto;
    }

    private CreationDto enrichCreationDto(CreationDto creationDto) {
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