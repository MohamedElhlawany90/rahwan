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

        // حساب العمولة وأرباح السائق
        double totalCost = cost.getTotalCost();
        double appCommission = round((totalCost * commissionRate) / 100.0);
        double driverEarnings = round(totalCost - appCommission);

        Order order = orderMapper.toEntity(orderForm);
        order.setUserId(userId);
        order.setDeliveryCost(totalCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setDriverEarnings(driverEarnings);
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
     * 2. تأكيد الطلب (إرسال OTP + status = PENDING)
     */
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getCreationStatus() != CreationStatus.CREATED) {
            throw new RuntimeException("Order must be in CREATED status to confirm");
        }

        // توليد OTP للاستلام (السائق هياخدها من User)
        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);

        // تحديث الحالة
        order.setStatus(OrderStatus.PENDING);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إرسال OTP للمستخدم
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            otpService.sendPickupOtp(user.getPhone(), "Your pickup OTP is: " + pickupOtp);

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
     */
    public CreationDto updateOrder(Long orderId, OrderForm orderForm, UUID userId) throws IOException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // جلب المستخدم للتحقق من نوعه
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // السماح بالتعديل إذا كان المستخدم هو صاحب الطلب أو أدمن
        if (!order.getUserId().equals(userId) && !currentUser.getType().equals("admin")) {
            throw new BusinessException("You are not allowed to update this order");
        }

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
        double appCommission = round((totalCost * commissionRate) / 100.0);
        double driverEarnings = round(totalCost - appCommission);

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
        order.setAdditionalNotes(orderForm.getAdditionalNotes());
        order.setCollectionDate(orderForm.getCollectionDate());
        order.setCollectionTime(orderForm.getCollectionTime());
        order.setAnyTime(orderForm.getAnyTime());
        order.setAllowInspection(orderForm.getAllowInspection());
        order.setReceiverPaysShipping(orderForm.getReceiverPaysShipping());

        // تحديث التكلفة والعمولة
        order.setDeliveryCost(totalCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setDriverEarnings(driverEarnings);

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
     */
    public OrderDto driverConfirmOrder(Long orderId, UUID driverId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
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
        order.setStatus(OrderStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إرسال OTP للمستلم
        otpService.sendDeliveryOtp(order.getRecipientPhone(), deliveryOtp);

        // إرسال تأكيد للـ User
        userRepository.findById(order.getUserId()).ifPresent(user -> whatsAppService.sendDriverAcceptedNotification(
                user.getPhone(),
                driver.getName(),
                order.getOtpForPickup()
        ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 5. السائق يستلم الطلب - هنا يحصل التجميد
     */
    public OrderDto confirmPickup(Long orderId, String otpFromUser) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new RuntimeException("Order must be in ACCEPTED status");
        }

        if (!order.getOtpForPickup().equals(otpFromUser)) {
            throw new RuntimeException("Invalid OTP for pickup");
        }

        // ✅ هنا يحصل التجميد بعد ما السائق يستلم

        // 1️⃣ تجميد ضعف التكلفة من User
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        double requiredBalance = order.getDeliveryCost() * 2;

        if (userWallet.getWalletBalance() < requiredBalance) {
            throw new RuntimeException("Insufficient balance. Required: " + requiredBalance +
                    ", Available: " + userWallet.getWalletBalance());
        }

        walletService.freezeAmount(userWallet, requiredBalance);

        // 2️⃣ تجميد قيمة التأمين من Driver
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());
        double insuranceValue = order.getInsuranceValue();

        if (driverWallet.getWalletBalance() < insuranceValue) {
            throw new RuntimeException("Driver has insufficient balance for insurance. Required: " +
                    insuranceValue + ", Available: " + driverWallet.getWalletBalance());
        }

        walletService.freezeAmount(driverWallet, insuranceValue);

        order.setPickupConfirmed(true);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 6. تحديث للحالة "في الطريق"
     */
    public OrderDto updateToInTheWay(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.IN_PROGRESS) {
            throw new RuntimeException("Order must be in IN_PROGRESS status");
        }

        order.setStatus(OrderStatus.IN_THE_WAY);
        Order updated = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 7. السائق يسلم الطلب - التحويلات المالية
     */
    public OrderDto confirmDelivery(Long orderId, String otpFromRecipient) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Pickup must be confirmed first");
        }

        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.IN_THE_WAY) {
            throw new RuntimeException("Order must be in progress");
        }

        if (!order.getOtpForDelivery().equals(otpFromRecipient)) {
            throw new RuntimeException("Invalid OTP for delivery");
        }

        order.setDeliveryConfirmed(true);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        // ✅ العمليات المالية الصحيحة:

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        // 1️⃣ فك تجميد ضعف التكلفة من User
        double totalFrozen = order.getDeliveryCost() * 2;
        walletService.unfreezeAmount(userWallet, totalFrozen);

        // 2️⃣ تحويل التأمين من Driver إلى User
        //    (السائق استلم الفلوس كاش، فالتأمين يروح للمستخدم)
        driverWallet.setFrozenBalance(driverWallet.getFrozenBalance() - order.getInsuranceValue());
        userWallet.setWalletBalance(userWallet.getWalletBalance() + order.getInsuranceValue());

        // 3️⃣ السائق يدفع عمولة التطبيق من محفظته
        //    (السائق أخذ الدليفري كاش، فيدفع العمولة من محفظته)
        if (driverWallet.getWalletBalance() < order.getAppCommission()) {
            throw new RuntimeException("Driver has insufficient balance to pay commission. Required: " +
                    order.getAppCommission() + ", Available: " + driverWallet.getWalletBalance());
        }

        driverWallet.setWalletBalance(driverWallet.getWalletBalance() - order.getAppCommission());

        // 4️⃣ حفظ المحافظ
        walletService.save(userWallet);
        walletService.save(driverWallet);

        // العمولة (appCommission) دُفعت من السائق وهي أرباح التطبيق

        Order updated = orderRepository.save(order);

        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDeliveryConfirmation(
                        user.getPhone(),
                        order.getTrackingNumber()
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 8. إرجاع الطلب
     */
    public OrderDto returnOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Cannot return order that wasn't picked up");
        }

        order.setStatus(OrderStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        // فك تجميد المبالغ
        double totalFrozen = order.getDeliveryCost() * 2;
        walletService.unfreezeAmount(userWallet, totalFrozen);
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        // السائق يأخذ أجرة الإرجاع كاملة (بدون عمولة)
        userWallet.setWalletBalance(userWallet.getWalletBalance() - order.getDeliveryCost());
        driverWallet.setWalletBalance(driverWallet.getWalletBalance() + order.getDeliveryCost());

        Order updated = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(updated));
    }

    // ... باقي الـ methods (getUserOrders, getDriverOrders, إلخ) بدون تغيير

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

        OrderStatisticsDto statisticsDto = new OrderStatisticsDto();
        statisticsDto.setTotalOrders(orders.size());
        statisticsDto.setPending(pending);
        statisticsDto.setAccepted(accepted);
        statisticsDto.setInProgress(inProgress);
        statisticsDto.setInTheWay(inTheWay);
        statisticsDto.setInDelivery(returned);
        statisticsDto.setDelivered(delivered);
        return statisticsDto;
    }

    public OrderDto changeOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(status);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toDto(savedOrder);
    }
}