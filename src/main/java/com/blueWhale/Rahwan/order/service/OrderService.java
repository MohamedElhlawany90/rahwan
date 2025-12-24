// ============================================
// OrderService.java (COMPLETE & FIXED)
// ============================================
package com.blueWhale.Rahwan.order.service;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.order.*;
import com.blueWhale.Rahwan.otp.OrderOtpService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final CostCalculationService costCalculationService;
    private final WalletService walletService;
    private final FileUploadService fileUploadService;
    private final OrderOtpService otpService;

    /**
     * 1. إنشاء طلب جديد من User
     */
    public OrderDto createOrder(OrderForm form, UUID userId, MultipartFile photo) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is not active");
        }

        // حساب التكلفة أولاً
        PricingDetails cost = costCalculationService.calculateCost(
                form.getPickupLatitude(),
                form.getPickupLongitude(),
                form.getRecipientLatitude(),
                form.getRecipientLongitude(),
                form.getInsuranceValue()
        );

        // التحقق من الرصيد (لازم يكون ضعف التكلفة)
        Wallet userWallet = walletService.getWalletByUserId(userId);
        double requiredBalance = cost.getTotalCost() * 2;

        if (userWallet.getBalance() < requiredBalance) {
            throw new RuntimeException("Insufficient balance. Required: " + requiredBalance +
                    ", Available: " + userWallet.getBalance());
        }

        // تجميد ضعف التكلفة
        walletService.freezeAmount(userWallet, requiredBalance);

        // إنشاء الطلب
        Order order = orderMapper.toEntity(form);
        order.setUserId(userId);
        order.setDeliveryCost(cost.getTotalCost());
        order.setDistanceKm(cost.getDistanceKm());
        order.setTrackingNumber(generateTrackingNumber());


        // رفع الصورة
        if (photo != null && !photo.isEmpty()) {
            String photoFileName = fileUploadService.saveOrderPhoto(photo);
            order.setPictureUrl(photoFileName);
        }

        order.setStatus(OrderStatus.CREATED);

        Order saved = orderRepository.save(order);
        return enrichDto(orderMapper.toDto(saved));
    }
    public OrderDto confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));


        order.setStatus(OrderStatus.PENDING);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Order is not in PENDING status");
        }

        order.setConfirmedAt(LocalDateTime.now());
        order.setTrackingNumber(generateTrackingNumber());

        Order updated = orderRepository.save(order);
        return orderMapper.toDto(updated);
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

        if (driverWallet.getBalance() < productValue) {
            throw new RuntimeException("Driver has insufficient balance for insurance. Required: " +
                    productValue + ", Available: " + driverWallet.getBalance());
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
            otpService.sendPickupOtp(user.getPhone(),
                    "Your order has been accepted by driver. Pickup OTP: " + order.getOtpForPickup());
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
        userWallet.setBalance(userWallet.getBalance() - order.getDeliveryCost());
        driverWallet.setBalance(driverWallet.getBalance() + order.getDeliveryCost());

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
                    .filter(o -> {
                        if (status == OrderStatus.PENDING) {
                            // skip CREATED and include PENDING and later statuses
                            return o.getStatus() != OrderStatus.CREATED;
                        } else {
                            return o.getStatus() == status;
                        }
                    })
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
     * Helper: إضافة أسماء المستخدمين
     */
    private OrderDto enrichDto(OrderDto dto) {
        if (dto.getUserId() != null) {
            userRepository.findById(dto.getUserId()).ifPresent(user ->
                    dto.setUserName(user.getName())
            );
        }
        if (dto.getDriverId() != null) {
            userRepository.findById(dto.getDriverId()).ifPresent(driver ->
                    dto.setDriverName(driver.getName())
            );
        }
        return dto;
    }

    /**
     * Helper: توليد Tracking Number
     */
    private String generateTrackingNumber() {
        return "ORD" + System.currentTimeMillis() +
                String.format("%04d", new Random().nextInt(10000));
    }



    public OrderStatusCounts getOrdersCountsByUser(UUID userId) {
    List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);

    long pending = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
    long accepted = orders.stream().filter(o -> o.getStatus() == OrderStatus.ACCEPTED).count();
    long inProgress = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
    long inTheWay = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_THE_WAY).count();
    long returned = orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURN).count();
    long delivered = orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count();

    long allOrders = pending + accepted + inProgress + inTheWay + returned + delivered; // or orders.size()
    long allActiveOrders = pending + accepted + inProgress + inTheWay;

    return new OrderStatusCounts(allOrders, allActiveOrders);
}

    }

