// ============================================
// OrderService.java (FIXED)
// ============================================
package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.notification.WhatsAppService;
import com.blueWhale.Rahwan.order.service.CostCalculationService;
import com.blueWhale.Rahwan.order.service.PricingDetails;
import com.blueWhale.Rahwan.otp.OrderOtpService;
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

        Order order = orderMapper.toEntity(orderForm);
        order.setUserId(userId);
        order.setDeliveryCost(cost.getTotalCost());
        order.setDistanceKm(cost.getDistanceKm());
        order.setTrackingNumber(generateTrackingNumber());
        order.setCreationStatus(CreationStatus.CREATED);
//        order.setStatus(OrderStatus.PENDING);

//        String pickupOtp = otpService.generatePickupOtp();
//        order.setOtpForPickup(pickupOtp);

        // هنا عرفنا uploadDir
        Path uploadDir = Paths.get(UPLOADED_FOLDER);


        if (orderForm.getPhoto() != null) {
            // لو الفولدر مش موجود نعمله
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            byte[] bytes = ImageUtility.compressImage(orderForm.getPhoto().getBytes());
            Path path = Paths
                    .get(UPLOADED_FOLDER + new Date().getTime() + "A-A" + orderForm.getPhoto().getOriginalFilename());
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
//        otpService.sendPickupOtp(user.getPhone(), "Your pickup OTP is: " + pickupOtp);

        return enrichCreationDto(orderMapper.toCreationDto(saved));
    }


    /**
     * 2. تأكيد الطلب (تجميد المبلغ + status = PENDING)
     */
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // التحقق من أن الطلب في حالة CREATED
        if (order.getCreationStatus() != CreationStatus.CREATED) {
            throw new RuntimeException("Order must be in CREATED status to confirm");
        }

        // التحقق من الرصيد (لازم يكون ضعف التكلفة)
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        double requiredBalance = order.getDeliveryCost() * 2;

        if (userWallet.getWalletBalance() < requiredBalance) {
            throw new RuntimeException("Insufficient balance. Required: " + requiredBalance +
                    ", Available: " + userWallet.getWalletBalance());
        }

        // تجميد ضعف التكلفة
        walletService.freezeAmount(userWallet, requiredBalance);

        // تحديث الحالة
        order.setStatus(OrderStatus.PENDING);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        // إرسال إشعار التأكيد
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            whatsAppService.sendOrderConfirmation(
                    user.getPhone(),
                    order.getTrackingNumber(),
                    order.getDeliveryCost()
            );
        }

        return enrichDto(orderMapper.toDto(updated));
    }

    public CreationDto updateOrder(Long orderId, OrderForm orderForm, UUID userId) throws IOException {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // تأكد إن الاوردر تابع لنفس اليوزر
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("You are not allowed to update this order");
        }

        // مينفعش نعدل بعد مراحل معينة
        if (order.getStatus() != null && order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Order cannot be updated in current status");
        }

        // إعادة حساب التكلفة
        PricingDetails cost = costCalculationService.calculateCost(
                orderForm.getPickupLatitude(),
                orderForm.getPickupLongitude(),
                orderForm.getRecipientLatitude(),
                orderForm.getRecipientLongitude(),
                orderForm.getInsuranceValue()
        );

        // تحديث البيانات العادية
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

        // تحديث التكلفة
        order.setDeliveryCost(cost.getTotalCost());
        order.setDistanceKm(cost.getDistanceKm());

        // 📸 تحديث الصورة (لو اتبعتت)
        Path uploadDir = Paths.get(UPLOADED_FOLDER);

        if (orderForm.getPhoto() != null && !orderForm.getPhoto().isEmpty()) {

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            byte[] bytes = ImageUtility.compressImage(orderForm.getPhoto().getBytes());

            String fileName = new Date().getTime()
                    + "A-A"
                    + orderForm.getPhoto().getOriginalFilename();

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
     * 3. السائق يقبل الطلب
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

        // تجميد قيمة المنتج من محفظة السائق
        Wallet driverWallet = walletService.getWalletByUserId(driverId);
        double productValue = order.getInsuranceValue();

        if (driverWallet.getWalletBalance() < productValue) {
            throw new RuntimeException("Driver has insufficient balance for insurance. Required: " +
                    productValue + ", Available: " + driverWallet.getWalletBalance());
        }

        walletService.freezeAmount(driverWallet, productValue);

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
        User user = userRepository.findById(order.getUserId()).orElse(null);
        if (user != null) {
            whatsAppService.sendDriverAcceptedNotification(
                    user.getPhone(),
                    driver.getName(),
                    order.getOtpForPickup()
            );
        }

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 4. السائق يستلم الطلب (يدخل OTP من User)
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

        order.setPickupConfirmed(true);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 5. السائق يسلم الطلب (يدخل OTP من Recipient)
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

        // العمليات المالية:
        double totalFrozen = order.getDeliveryCost() * 2;
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        walletService.unfreezeAmount(userWallet, totalFrozen);

        walletService.transferFrozenAmount(order.getDriverId(), order.getUserId(), order.getInsuranceValue());

        Order updated = orderRepository.save(order);

        // إرسال تأكيد التسليم
        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDeliveryConfirmation(
                user.getPhone(),
                order.getTrackingNumber()
        ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 6. إرجاع الطلب
     */
    public OrderDto returnOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isPickupConfirmed()) {
            throw new RuntimeException("Cannot return order that wasn't picked up");
        }

        order.setStatus(OrderStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        // فك تجميد المبلغ من User
        double totalFrozen = order.getDeliveryCost() * 2;
        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        walletService.unfreezeAmount(userWallet, totalFrozen);

        // فك تجميد قيمة المنتج من Driver
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        // تحويل تكلفة التوصيل من User إلى Driver
        userWallet.setWalletBalance(userWallet.getWalletBalance() - order.getDeliveryCost());
        driverWallet.setWalletBalance(driverWallet.getFrozenBalance() + order.getDeliveryCost());

        Order updated = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 7. تحديث للحالة "في الطريق"
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
     * 8. جلب طلبات المستخدم
     */
    public List<OrderDto> getUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * 9. جلب طلبات السائق
     */
    public List<OrderDto> getDriverOrders(UUID driverId) {
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * 10. جلب الطلبات المتاحة
     */
    public List<OrderDto> getAvailableOrders() {
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * 11. جلب طلبات حسب الحالة والمستخدم
     */
    public List<OrderDto> getOrdersByUserAndStatus(UUID userId, OrderStatus status) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(order -> order.getStatus() == status)
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }

    /**
     * 12. جلب طلب بالـ ID
     */
        public OrderDto getOrderById(Long orderId) {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            return enrichDto(orderMapper.toDto(order));
        }

    /**
     * 13. جلب طلب بالـ Tracking Number
     */
    public OrderDto getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber));
        return enrichDto(orderMapper.toDto(order));
    }

    /**
     * 14. جلب إحصائيات الطلبات
     */
    public OrderStatusCounts getOrdersCountsByUser(UUID userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long pending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long accepted = orders.stream().filter(o -> o.getStatus() == OrderStatus.ACCEPTED).count();
        long inProgress = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
        long inTheWay = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_THE_WAY).count();
        long returned = orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURN).count();
        long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

//        long allOrders = orders.stream()
//                .filter(o -> o.getStatus() != null)
//                .count();

        long allOrders = orders.size();
        long allActiveOrders = pending + accepted + inProgress + inTheWay;

        return new OrderStatusCounts(allOrders, allActiveOrders);
    }

    /**
     * Helper: إضافة أسماء المستخدمين
     */
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

    /**
     * Helper: توليد Tracking Number
     */
    private String generateTrackingNumber() {
        return "ORD" + System.currentTimeMillis() +
                String.format("%04d", new Random().nextInt(10000));
    }
    public List<OrderDto> getAllOrders(){
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toDto)
                .map(this::enrichDto)
                .collect(Collectors.toList());
    }
    public OrderStatisticsDto getOrderStatistics(UUID userId) {

        List<Order> orders = orderRepository.findByUserId(userId) ;

        long pending = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.PENDING).count();
        long accepted = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.ACCEPTED).count();
        long inProgress = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.IN_PROGRESS).count();
        long inTheWay = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.IN_THE_WAY).count();
        long returned = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.RETURN).count();
        long delivered = orders.stream()
                .filter(order -> order.getStatus()== OrderStatus.DELIVERED).count();

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

        // تغيير الحالة يدوي
        order.setStatus(status);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toDto(savedOrder);
    }

}