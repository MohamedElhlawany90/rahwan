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

    // ==================== Helper: Role Checks ====================

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!user.isActive()) throw new BusinessException("Account is not active");
        return user;
    }

    private void requireUserRole(User user) {
        if (!user.isUser())
            throw new BusinessException("This action is only allowed for users");
    }

    private void requireDriverRole(User user) {
        if (!user.isDriver())
            throw new BusinessException("This action is only allowed for drivers");
    }

    private void requireAdminRole(User user) {
        if (!user.isAdmin())
            throw new BusinessException("This action is only allowed for admins");
    }

    // ==================== Order Operations ====================

    /**
     * 1. إنشاء طلب جديد
     * Authorization: USER role فقط
     */
    public CreationDto createOrder(OrderForm orderForm, UUID userId) throws IOException {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅ السواق الـ pure مينفعش يعمل أوردر

        PricingDetails cost = costCalculationService.calculateCost(
                orderForm.getPickupLatitude(), orderForm.getPickupLongitude(),
                orderForm.getRecipientLatitude(), orderForm.getRecipientLongitude(),
                orderForm.getInsuranceValue()
        );

        CommissionSettings commissionSettings = commissionSettingsService.getActiveSettings();
        double commissionRate = commissionSettings.getCommissionRate();
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
        order.setStatus(OrderStatus.PENDING);

        Path uploadDir = Paths.get(UPLOADED_FOLDER);
        if (orderForm.getPhoto() != null) {
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
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

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    /**
     * 2. تأكيد الطلب (تجميد المبلغ)
     * Authorization: USER role (صاحب الطلب فقط)
     */
    public OrderDto confirmOrder(Long orderId, UUID userId) {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getUserId().equals(userId))
            throw new BusinessException("You are not the owner of this order");

        if (order.getCreationStatus() != CreationStatus.CREATED)
            throw new RuntimeException("Order must be in CREATED status to confirm");

        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        walletService.freezeAmount(userWallet, order.getDeliveryCost() * 2);

        order.setStatus(OrderStatus.PENDING);
        order.setCreationStatus(CreationStatus.CONFIRMED); // ✅ mark as user-confirmed
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        whatsAppService.sendPickupOtpToSender(user.getPhone(), user.getName(), pickupOtp);
        whatsAppService.sendOrderConfirmation(user.getPhone(), order.getTrackingNumber(), order.getDeliveryCost());

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 3. تحديث الطلب
     * Authorization: USER role (صاحب الطلب) أو ADMIN
     */
    public CreationDto updateOrder(Long orderId, OrderForm orderForm, UUID userId) throws IOException {
        User currentUser = getActiveUser(userId);

        if (!currentUser.isUser() && !currentUser.isAdmin()) // ✅ لازم يكون user أو admin
            throw new BusinessException("This action is only allowed for users or admins");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId) && !currentUser.isAdmin())
            throw new BusinessException("You are not allowed to update this order");

        if (order.getStatus() != null && order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Order cannot be updated in current status");

        PricingDetails cost = costCalculationService.calculateCost(
                orderForm.getPickupLatitude(), orderForm.getPickupLongitude(),
                orderForm.getRecipientLatitude(), orderForm.getRecipientLongitude(),
                orderForm.getInsuranceValue()
        );

        CommissionSettings commissionSettings = commissionSettingsService.getActiveSettings();
        double commissionRate = commissionSettings.getCommissionRate();
        double totalCost = cost.getTotalCost();
        double appCommission = round((orderForm.getInsuranceValue() * commissionRate) / 100.0);

        if (order.getStatus() == OrderStatus.PENDING) {
            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
            walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);
            walletService.freezeAmount(userWallet, totalCost * 2);
        }

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

        Path uploadDir = Paths.get(UPLOADED_FOLDER);
        if (orderForm.getPhoto() != null && !orderForm.getPhoto().isEmpty()) {
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);
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

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    /**
     * 4. السائق يقبل الطلب
     * Authorization: DRIVER role فقط
     */
    public OrderDto driverConfirmOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅ موجودة بالفعل

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Order cannot be accepted in current status");

        if (order.getCreationStatus() != CreationStatus.CONFIRMED) // ✅ must be user-confirmed first
            throw new BusinessException("Order has not been confirmed by the user yet and cannot be accepted");

        if (order.getDriverId() != null)
            throw new BusinessException("Order already accepted by another driver");

        Wallet driverWallet = walletService.getWalletByUserId(driverId);
        if (driverWallet.getWalletBalance() < order.getInsuranceValue())
            throw new BusinessException("Insufficient balance. You need at least " + order.getInsuranceValue() +
                    " EGP to accept this order. Current balance: " + driverWallet.getWalletBalance() + " EGP");

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDriverAcceptedNotification(
                        user.getPhone(), driver.getName(), order.getTrackingNumber()
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 5. السائق يؤكد الاستلام بـ OTP
     * Authorization: DRIVER role + صاحب الطلب
     */
    public OrderDto confirmPickup(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the driver who accepted this order can confirm pickup");

        if (order.getStatus() != OrderStatus.ACCEPTED)
            throw new BusinessException("Order must be in ACCEPTED status");

        if (!otp.equals(order.getOtpForPickup()))
            throw new BusinessException("Invalid OTP");

        String deliveryOtp = otpService.generateDeliveryOtp();
        order.setOtpForDelivery(deliveryOtp);

        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());
        walletService.freezeAmount(driverWallet, order.getInsuranceValue());

        order.setPickupConfirmed(true);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        Order updated = orderRepository.save(order);

        whatsAppService.sendDeliveryOtpToRecipient(order.getRecipientPhone(), order.getRecipientName(), deliveryOtp);

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 6. السائق يؤكد التسليم بـ OTP
     * Authorization: DRIVER role + صاحب الطلب
     */
    public OrderDto confirmDelivery(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the driver who accepted this order can confirm delivery");

        if (order.getStatus() != OrderStatus.IN_PROGRESS && order.getStatus() != OrderStatus.IN_THE_WAY)
            throw new BusinessException("Order must be IN_PROGRESS or IN_THE_WAY");

        if (!otp.equals(order.getOtpForDelivery()))
            throw new BusinessException("Invalid delivery OTP");

        order.setDeliveryConfirmed(true);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        double userReceives = order.getInsuranceValue() - order.getAppCommission();

        if (driverWallet.getWalletBalance() < order.getInsuranceValue())
            throw new BusinessException("Driver has insufficient balance");

        driverWallet.setWalletBalance(driverWallet.getWalletBalance() - order.getInsuranceValue());
        userWallet.setWalletBalance(userWallet.getWalletBalance() + userReceives);

        walletService.save(userWallet);
        walletService.save(driverWallet);

        Order updated = orderRepository.save(order);

        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendDeliveryConfirmation(user.getPhone(), order.getTrackingNumber()));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 7. السائق يرجع الطلب
     * Authorization: DRIVER role + صاحب الطلب
     */
    public OrderDto returnOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the driver who accepted this order can return it");

        if (!order.isPickupConfirmed())
            throw new BusinessException("Cannot return order that wasn't picked up");

        order.setStatus(OrderStatus.RETURN);
        order.setDeliveredAt(LocalDateTime.now());

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        Order updated = orderRepository.save(order);

        userRepository.findById(order.getUserId())
                .ifPresent(user -> whatsAppService.sendOrderCancellation(
                        user.getPhone(), order.getTrackingNumber(), "Order returned to sender"
                ));

        return enrichDto(orderMapper.toDto(updated));
    }

    /**
     * 8. السائق يلغي الطلب
     * Authorization: DRIVER role فقط
     */
    public OrderDto cancelOrderByDriver(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅ موجودة بالفعل

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED)
            throw new BusinessException("Order cannot be cancelled. Current status: " + order.getStatus());

        Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
        walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

        order.setStatus(OrderStatus.CANCELLED);
        return enrichDto(orderMapper.toDto(orderRepository.save(order)));
    }

    /**
     * 9. المستخدم يلغي الطلب
     * Authorization: USER role (صاحب الطلب) أو ADMIN
     */
    public OrderDto cancelOrderByUser(Long orderId, UUID userId, String cancellationReason) {
        User user = getActiveUser(userId);

        if (!user.isUser() && !user.isAdmin()) // ✅ السواق الـ pure مينفعش يلغي طلب يوزر
            throw new BusinessException("This action is only allowed for users or admins");

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(userId) && !user.isAdmin())
            throw new BusinessException("You are not authorized to cancel this order");

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.RETURN)
            throw new BusinessException("Order cannot be cancelled at this stage: " + order.getStatus());

        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new BusinessException("Order is already cancelled");

        // السيناريو 1: PENDING ولم يقبله سائق
        if (order.getStatus() == OrderStatus.PENDING && order.getDriverId() == null) {
            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
            walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(cancellationReason);

            Order updated = orderRepository.save(order);
            whatsAppService.sendOrderCancellation(user.getPhone(), order.getTrackingNumber(),
                    cancellationReason != null ? cancellationReason : "Cancelled by user");

            return enrichDto(orderMapper.toDto(updated));
        }

        // السيناريو 2: السائق قبل الطلب - دفع تعويض
        if (order.getDriverId() != null &&
                (order.getStatus() == OrderStatus.ACCEPTED || order.getStatus() == OrderStatus.IN_PROGRESS)) {

            Wallet userWallet = walletService.getWalletByUserId(order.getUserId());
            Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

            walletService.unfreezeAmount(userWallet, order.getDeliveryCost() * 2);

            if (order.getStatus() == OrderStatus.IN_PROGRESS)
                walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

            if (userWallet.getWalletBalance() < order.getDeliveryCost())
                throw new BusinessException("Insufficient balance to cancel order. Required: " +
                        order.getDeliveryCost() + ", Available: " + userWallet.getWalletBalance());

            userWallet.setWalletBalance(userWallet.getWalletBalance() - order.getDeliveryCost());
            driverWallet.setWalletBalance(driverWallet.getWalletBalance() + order.getDeliveryCost());

            walletService.save(userWallet);
            walletService.save(driverWallet);

            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(cancellationReason != null ? cancellationReason : "Cancelled by user after driver acceptance");

            Order updated = orderRepository.save(order);

            userRepository.findById(order.getDriverId())
                    .ifPresent(driver -> whatsAppService.sendOrderCancellation(
                            driver.getPhone(), order.getTrackingNumber(),
                            "User cancelled the order. You received " + order.getDeliveryCost() + " EGP as compensation"
                    ));

            return enrichDto(orderMapper.toDto(updated));
        }

        throw new BusinessException("Unable to cancel order in current state");
    }

    /**
     * 10. السائق يحدث "في الطريق"
     * Authorization: DRIVER role + صاحب الطلب
     */
    public OrderDto updateToInTheWay(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the driver who accepted this order can update status");

        if (order.getStatus() != OrderStatus.IN_PROGRESS)
            throw new BusinessException("Order must be in IN_PROGRESS status");

        order.setStatus(OrderStatus.IN_THE_WAY);
        return enrichDto(orderMapper.toDto(orderRepository.save(order)));
    }

    /**
     * 11. Admin: تغيير حالة الطلب
     * Authorization: ADMIN role فقط
     */
    public OrderDto changeOrderStatus(Long orderId, OrderStatus status, UUID adminId) {
        User admin = getActiveUser(adminId);
        requireAdminRole(admin); // ✅

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(status);
        return orderMapper.toDto(orderRepository.save(order));
    }

    // ==================== Query Methods ====================

    /**
     * Authorization: USER role فقط
     */
    public List<OrderDto> getUserOrders(UUID userId) {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    /**
     * Authorization: DRIVER role فقط
     */
    public List<OrderDto> getDriverOrders(UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream().map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    /**
     * Authorization: DRIVER role فقط
     */
    public List<OrderDto> getAvailableOrders(UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriverRole(driver); // ✅

        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED) // ✅ hide CREATED orders
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    /**
     * Authorization: USER role فقط
     */
    public List<OrderDto> getOrdersByUserAndStatus(UUID userId, OrderStatus status) {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(order -> order.getStatus() == status)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    // Public - بدون role check
    public DriverDto getOrderByIdAsDriverDto(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getCreationStatus() == CreationStatus.CREATED) // ✅ hide unconfirmed
            throw new ResourceNotFoundException("Order not found");
        return enrichDriverDto(orderMapper.toDriverDto(order));
    }

    // Public - تتبع بالـ tracking number بدون توكين
    public OrderDto getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber));
        if (order.getCreationStatus() == CreationStatus.CREATED) // ✅ hide unconfirmed
            throw new ResourceNotFoundException("Order not found with tracking number: " + trackingNumber);
        return enrichDto(orderMapper.toDto(order));
    }

    /**
     * Authorization: USER role فقط
     */
    public OrderStatusCounts getOrdersCountsByUser(UUID userId) {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅

        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long pending    = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long accepted   = orders.stream().filter(o -> o.getStatus() == OrderStatus.ACCEPTED).count();
        long inProgress = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count();
        long inTheWay   = orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_THE_WAY).count();
        long allOrders  = orders.size();
        long allActive  = pending + accepted + inProgress + inTheWay;
        return new OrderStatusCounts(allOrders, allActive);
    }

    /**
     * Authorization: ADMIN role فقط
     */
    public List<OrderDto> getAllOrders() {
//        User user = getActiveUser(userId);
//        requireAdminRole(user); // ✅

        return orderRepository.findAll()
                .stream().map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    /**
     * Authorization: USER role فقط
     */
    public OrderStatisticsDto getOrderStatistics(UUID userId) {
        User user = getActiveUser(userId);
        requireUserRole(user); // ✅

        List<Order> orders = orderRepository.findByUserId(userId);
        OrderStatisticsDto dto = new OrderStatisticsDto();
        dto.setTotalOrders(orders.size());
        dto.setPending(orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count());
        dto.setAccepted(orders.stream().filter(o -> o.getStatus() == OrderStatus.ACCEPTED).count());
        dto.setInProgress(orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_PROGRESS).count());
        dto.setInTheWay(orders.stream().filter(o -> o.getStatus() == OrderStatus.IN_THE_WAY).count());
        dto.setInDelivery(orders.stream().filter(o -> o.getStatus() == OrderStatus.RETURN).count());
        dto.setDelivered(orders.stream().filter(o -> o.getStatus() == OrderStatus.DELIVERED).count());
        dto.setCancelled(orders.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count());
        return dto;
    }

    // ==================== Helper Methods ====================

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String generateTrackingNumber() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    private OrderDto enrichDto(OrderDto orderDto) {
        if (orderDto.getUserId() != null)
            userRepository.findById(orderDto.getUserId()).ifPresent(u -> orderDto.setUserName(u.getName()));
        if (orderDto.getDriverId() != null)
            userRepository.findById(orderDto.getDriverId()).ifPresent(d -> orderDto.setDriverName(d.getName()));
        return orderDto;
    }

    private CreationDto enrichCreationDto(CreationDto dto) {
        if (dto.getUserId() != null)
            userRepository.findById(dto.getUserId()).ifPresent(u -> dto.setUserName(u.getName()));
        if (dto.getDriverId() != null)
            userRepository.findById(dto.getDriverId()).ifPresent(d -> dto.setDriverName(d.getName()));
        return dto;
    }

    private DriverDto enrichDriverDto(DriverDto dto) {
        if (dto.getUserId() != null)
            userRepository.findById(dto.getUserId()).ifPresent(u -> dto.setUserName(u.getName()));
        if (dto.getDriverId() != null)
            userRepository.findById(dto.getDriverId()).ifPresent(d -> {
                dto.setDriverName(d.getName());
                dto.setDriverPhoto(d.getProfileImage());
            });
        return dto;
    }
}