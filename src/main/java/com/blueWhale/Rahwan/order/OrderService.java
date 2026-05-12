package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.commission.CommissionSettings;
import com.blueWhale.Rahwan.commission.CommissionSettingsService;
import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.order.service.CostCalculationService;
import com.blueWhale.Rahwan.order.service.PricingDetails;
import com.blueWhale.Rahwan.otp.OrderOtpService;
import com.blueWhale.Rahwan.user.User;
import com.blueWhale.Rahwan.user.UserRepository;
import com.blueWhale.Rahwan.util.StorageService;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OrderService is now a pure orchestrator.
 * It does NOT contain:
 *   - File I/O  → delegated to StorageService
 *   - Money ops → delegated to OrderFinanceService
 *   - WhatsApp  → published as events, handled by OrderNotificationListener
 *   - Math      → delegated to OrderFinanceService
 *
 * It ONLY:
 *   1. Validates business rules
 *   2. Changes Order state
 *   3. Saves to DB
 *   4. Delegates side effects
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository          orderRepository;
    private final OrderMapper              orderMapper;
    private final UserRepository           userRepository;
    private final CostCalculationService   costCalculationService;
    private final CommissionSettingsService commissionSettingsService;
    private final WalletService            walletService;
    private final OrderOtpService          otpService;
    private final StorageService           storageService;       // ✅ replaces inline file code
    private final OrderFinanceService      financeService;       // ✅ replaces inline wallet code
    private final ApplicationEventPublisher eventPublisher;      // ✅ replaces inline WhatsApp calls

    // ═══════════════════════════════════════════════════════════════════════
    //  Role Guards
    // ═══════════════════════════════════════════════════════════════════════

    private User getActiveUser(UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!u.isActive()) throw new BusinessException("Account is not active");
        return u;
    }

    private void requireUser(User u)   { if (!u.isUser())   throw new BusinessException("Users only");   }
    private void requireDriver(User u) { if (!u.isDriver()) throw new BusinessException("Drivers only"); }
    private void requireAdmin(User u)  { if (!u.isAdmin())  throw new BusinessException("Admins only");  }

    // ═══════════════════════════════════════════════════════════════════════
    //  1. Create Order
    // ═══════════════════════════════════════════════════════════════════════

    public CreationDto createOrder(OrderForm form, UUID userId) throws IOException {
        User user = getActiveUser(userId);
        requireUser(user);

        PricingDetails cost = costCalculationService.calculateCost(
                form.getPickupLatitude(), form.getPickupLongitude(),
                form.getRecipientLatitude(), form.getRecipientLongitude(),
                form.getInsuranceValue());

        CommissionSettings commission = commissionSettingsService.getActiveSettings();
        double deliveryCost   = cost.getTotalCost();
        double commissionRate = commission.getCommissionRate();

        // ✅ FIX 1: Commission calculated on deliveryCost, not insuranceValue
        double appCommission  = financeService.calculateCommission(deliveryCost, commissionRate);
        // ✅ FIX 2: Driver earnings persisted immediately on the order
        double driverEarnings = financeService.calculateDriverEarnings(deliveryCost, appCommission);

        // Balance pre-check before order is even created
        Wallet userWallet = walletService.getWalletByUserId(userId);
        double required = financeService.calculateFreezeAmount(deliveryCost);
        if (userWallet.getWalletBalance() < required)
            throw new BusinessException("Insufficient balance. Need at least " + required + " EGP.");

        Order order = orderMapper.toEntity(form);
        order.setUserId(userId);
        order.setDeliveryCost(deliveryCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setDriverEarnings(driverEarnings);          // ✅ FIX 2 applied
        order.setTrackingNumber(generateTrackingNumber());
        order.setCreationStatus(CreationStatus.CREATED);
        order.setStatus(OrderStatus.PENDING);
        order.setPhoto(storageService.savePhoto(form.getPhoto())); // ✅ delegated

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Confirm Order  (freeze wallet)
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto confirmOrder(Long orderId, UUID userId) {
        User user = getActiveUser(userId);
        requireUser(user);

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId))
            throw new BusinessException("You are not the owner of this order");
        if (order.getCreationStatus() != CreationStatus.CREATED)
            throw new BusinessException("Order must be in CREATED status to confirm");

        String pickupOtp = otpService.generatePickupOtp();
        order.setOtpForPickup(pickupOtp);
        order.setStatus(OrderStatus.PENDING);
        order.setCreationStatus(CreationStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());

        financeService.freezeForOrder(userId, order.getDeliveryCost()); // ✅ delegated
        Order saved = orderRepository.save(order);

        publish(saved, OrderNotificationEvent.Type.ORDER_CONFIRMED, null, pickupOtp); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. Update Order
    // ═══════════════════════════════════════════════════════════════════════

    public CreationDto updateOrder(Long orderId, OrderForm form, UUID userId) throws IOException {
        User actor = getActiveUser(userId);
        if (!actor.isUser() && !actor.isAdmin())
            throw new BusinessException("Users or admins only");

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId) && !actor.isAdmin())
            throw new BusinessException("You are not allowed to update this order");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Order cannot be updated in current status");

        PricingDetails cost = costCalculationService.calculateCost(
                form.getPickupLatitude(), form.getPickupLongitude(),
                form.getRecipientLatitude(), form.getRecipientLongitude(),
                form.getInsuranceValue());

        CommissionSettings commission = commissionSettingsService.getActiveSettings();
        double newDeliveryCost  = cost.getTotalCost();
        double commissionRate   = commission.getCommissionRate();
        double appCommission    = financeService.calculateCommission(newDeliveryCost, commissionRate);
        double driverEarnings   = financeService.calculateDriverEarnings(newDeliveryCost, appCommission);

        // Swap frozen amount
        financeService.refreezeForOrder(order.getUserId(), order.getDeliveryCost(), newDeliveryCost);

        // Update all fields
        order.setPickupLatitude(form.getPickupLatitude());
        order.setPickupLongitude(form.getPickupLongitude());
        order.setPickupAddress(form.getPickupAddress());
        order.setRecipientLatitude(form.getRecipientLatitude());
        order.setRecipientLongitude(form.getRecipientLongitude());
        order.setRecipientAddress(form.getRecipientAddress());
        order.setRecipientName(form.getRecipientName());
        order.setRecipientPhone(form.getRecipientPhone());
        order.setOrderType(form.getOrderType());
        order.setInsuranceValue(form.getInsuranceValue());
        order.setDeliveryCost(newDeliveryCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setDriverEarnings(driverEarnings);              // ✅ FIX 2 applied
        order.setAdditionalNotes(form.getAdditionalNotes());
        order.setCollectionDate(form.getCollectionDate());
        order.setCollectionTime(form.getCollectionTime());
        order.setAnyTime(form.getAnyTime());
        order.setAllowInspection(form.getAllowInspection());
        order.setReceiverPaysShipping(form.getReceiverPaysShipping());

        String newPhoto = storageService.savePhoto(form.getPhoto());
        if (newPhoto != null) order.setPhoto(newPhoto);

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. Driver Accepts
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto driverConfirmOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCreationStatus() != CreationStatus.CONFIRMED)
            throw new BusinessException("Order not yet confirmed by the user");
        if (order.getDriverId() != null)
            throw new BusinessException("Order already taken by another driver");

        Wallet driverWallet = walletService.getWalletByUserId(driverId);
        if (driverWallet.getWalletBalance() < order.getInsuranceValue())
            throw new BusinessException("Insufficient balance. Need at least "
                    + order.getInsuranceValue() + " EGP to accept this order.");

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.DRIVER_ACCEPTED, driverId, null); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  5. Confirm Pickup  (freeze driver insurance)
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto confirmPickup(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can confirm pickup");
        if (order.getStatus() != OrderStatus.ACCEPTED)
            throw new BusinessException("Order must be ACCEPTED");
        if (!otp.equals(order.getOtpForPickup()))
            throw new BusinessException("Invalid OTP");

        String deliveryOtp = otpService.generateDeliveryOtp();
        order.setOtpForDelivery(deliveryOtp);
        order.setPickupConfirmed(true);
        order.setStatus(OrderStatus.IN_PROGRESS);
        order.setPickedUpAt(LocalDateTime.now());

        financeService.freezeDriverInsurance(driverId, order.getInsuranceValue()); // ✅ delegated
        Order saved = orderRepository.save(order);

        publish(saved, OrderNotificationEvent.Type.DELIVERY_OTP_SENT, null, deliveryOtp); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  6. Confirm Delivery  (settle all money)
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto confirmDelivery(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can confirm delivery");
        if (order.getStatus() != OrderStatus.IN_PROGRESS)
            throw new BusinessException("Order must be IN_PROGRESS");
        if (!otp.equals(order.getOtpForDelivery()))
            throw new BusinessException("Invalid delivery OTP");

        order.setDeliveryConfirmed(true);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        financeService.settleDelivery(saved); // ✅ all money logic in one place
        publish(saved, OrderNotificationEvent.Type.DELIVERY_CONFIRMED, null, null); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7-A. Return — generate OTP
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto returnOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can return this order");
        if (!order.isPickupConfirmed())
            throw new BusinessException("Cannot return order that wasn't picked up");
        if (order.getStatus() == OrderStatus.IN_RETURN)
            throw new BusinessException("Return already initiated");

        String returnOtp = otpService.generateReturnOtp();
        order.setOtpForReturn(returnOtp);
        order.setStatus(OrderStatus.IN_RETURN);

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.RETURN_INITIATED, null, returnOtp); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7-B. Confirm Return — settle money
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto confirmReturn(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can confirm return");
        if (order.getStatus() != OrderStatus.IN_RETURN)
            throw new BusinessException("Order must be IN_RETURN");
        if (order.getOtpForReturn() == null || !order.getOtpForReturn().equals(otp))
            throw new BusinessException("Invalid return OTP");

        order.setStatus(OrderStatus.RETURNED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        financeService.settleReturn(saved); // ✅ all money logic in one place
        publish(saved, OrderNotificationEvent.Type.RETURN_CONFIRMED, null, null); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  8. Driver Cancels
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto cancelOrderByDriver(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!driverId.equals(order.getDriverId()))
            throw new BusinessException("You are not the assigned driver");
        if (order.getStatus() != OrderStatus.ACCEPTED)
            throw new BusinessException("Can only cancel at ACCEPTED status");

        order.setDriverId(null);
        order.setStatus(OrderStatus.PENDING);
        order.setConfirmedAt(null);

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.DRIVER_CANCELLED, driverId, null); // ✅ FIX 4: notifies user
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  9. User Cancels
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto cancelOrderByUser(Long orderId, UUID userId, String reason) {
        User user = getActiveUser(userId);
        if (!user.isUser() && !user.isAdmin())
            throw new BusinessException("Users or admins only");

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId) && !user.isAdmin())
            throw new BusinessException("Not authorized to cancel this order");
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.IN_RETURN)
            throw new BusinessException("Cannot cancel at stage: " + order.getStatus());
        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new BusinessException("Order already cancelled");

        // Scenario 1: no driver yet → simple unfreeze
        if (order.getDriverId() == null) {
            financeService.unfreezeForCancel(order.getUserId(), order.getDeliveryCost());
            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(reason);
            Order saved = orderRepository.save(order);
            publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_NO_DRIVER, null, reason); // ✅ event
            return enrichDto(orderMapper.toDto(saved));
        }

        // Scenario 2: driver accepted → penalty
        financeService.settleCancellationWithPenalty(order); // ✅ delegated
        order.setStatus(OrderStatus.CANCELLED);
        order.setRejectionReason(reason != null ? reason : "Cancelled by user after driver acceptance");
        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_WITH_DRIVER, null, null); // ✅ event
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  10. Admin: Change Status
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto changeOrderStatus(Long orderId, OrderStatus status, UUID adminId) {
        requireAdmin(getActiveUser(adminId));
        Order order = findOrder(orderId);
        order.setStatus(status);
        return orderMapper.toDto(orderRepository.save(order));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Queries
    // ═══════════════════════════════════════════════════════════════════════

    public List<OrderDto> getUserOrders(UUID userId) {
        requireUser(getActiveUser(userId));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<OrderDto> getDriverOrders(UUID driverId) {
        requireDriver(getActiveUser(driverId));
        return orderRepository.findByDriverIdOrderByCreatedAtDesc(driverId)
                .stream().map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<OrderDto> getAvailableOrders(UUID driverId) {
        requireDriver(getActiveUser(driverId));
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public List<OrderDto> getOrdersByUserAndStatus(UUID userId, OrderStatus status) {
        requireUser(getActiveUser(userId));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().filter(o -> o.getStatus() == status)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public DriverDto getOrderByIdAsDriverDto(Long orderId) {
        Order order = findOrder(orderId);
        if (order.getCreationStatus() == CreationStatus.CREATED)
            throw new ResourceNotFoundException("Order not found");
        return enrichDriverDto(orderMapper.toDriverDto(order));
    }

    public OrderDto getOrderByTrackingNumber(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getCreationStatus() == CreationStatus.CREATED)
            throw new ResourceNotFoundException("Order not found");
        return enrichDto(orderMapper.toDto(order));
    }

    public OrderStatusCounts getOrdersCountsByUser(UUID userId) {
        requireUser(getActiveUser(userId));
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long active = orders.stream().filter(o ->
                o.getStatus() == OrderStatus.PENDING   ||
                        o.getStatus() == OrderStatus.ACCEPTED  ||
                        o.getStatus() == OrderStatus.IN_PROGRESS).count();
        return new OrderStatusCounts(orders.size(), active);
    }

    // Admin-only: return all confirmed orders in the system
    public List<OrderDto> getAllOrders(UUID adminId) {
        requireAdmin(getActiveUser(adminId));
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    // For current user: return their confirmed orders only
    public List<OrderDto> getAllOrdersForUser(UUID userId) {
        requireUser(getActiveUser(userId));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

    public OrderStatisticsDto getOrderStatistics(UUID userId) {
        requireUser(getActiveUser(userId));
        List<Order> orders = orderRepository.findByUserId(userId);
        OrderStatisticsDto dto = new OrderStatisticsDto();
        dto.setTotalOrders(orders.size());
        dto.setPending(  count(orders, OrderStatus.PENDING));
        dto.setAccepted( count(orders, OrderStatus.ACCEPTED));
        dto.setInProgress(count(orders, OrderStatus.IN_PROGRESS));
        dto.setInTheWay( 0); // removed per requirements
        dto.setInDelivery(count(orders, OrderStatus.RETURNED));
        dto.setDelivered(count(orders, OrderStatus.DELIVERED));
        dto.setCancelled(count(orders, OrderStatus.CANCELLED));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    /**
     * ✅ FIX: Unified name resolution — used by all three enrich methods.
     * One DB lookup pattern instead of three copy-pasted blocks.
     */
    private String resolveName(UUID id) {
        if (id == null) return null;
        return userRepository.findById(id).map(User::getName).orElse(null);
    }

    private OrderDto enrichDto(OrderDto dto) {
        dto.setUserName(resolveName(dto.getUserId()));
        dto.setDriverName(resolveName(dto.getDriverId()));
        return dto;
    }

    private CreationDto enrichCreationDto(CreationDto dto) {
        dto.setUserName(resolveName(dto.getUserId()));
        dto.setDriverName(resolveName(dto.getDriverId()));
        return dto;
    }

    private DriverDto enrichDriverDto(DriverDto dto) {
        dto.setUserName(resolveName(dto.getUserId()));
        if (dto.getDriverId() != null)
            userRepository.findById(dto.getDriverId()).ifPresent(d -> {
                dto.setDriverName(d.getName());
                dto.setDriverPhoto(d.getProfileImage());
            });
        return dto;
    }

    private void publish(Order order, OrderNotificationEvent.Type type, UUID actorId, String payload) {
        eventPublisher.publishEvent(
                new OrderNotificationEvent(this, order, type, actorId, payload));
    }

    private long count(List<Order> orders, OrderStatus status) {
        return orders.stream().filter(o -> o.getStatus() == status).count();
    }

    private String generateTrackingNumber() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }
}