package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.charity.Charity;
import com.blueWhale.Rahwan.charity.CharityRepository;
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
 * Unified service for ALL order types.
 *
 * OrderCategory.REGULAR — standard paid delivery
 *   • User wallet is frozen 2× at confirmation
 *   • Driver insurance is frozen at pickup
 *   • On delivery: user pays, driver earns net (deliمveryCost − appCommission)
 *   • On return:   user pays 2× penalty to driver
 *   • On cancel (after driver): user pays 1× penalty to driver
 *
 * OrderCategory.CHARITY — WasalElkheer donation delivery
 *   • No wallet freeze on user (items donated, not money)
 *   • No insurance freeze on driver
 *   • On delivery: app credits driver full deliveryCost (no commission split)
 *   • No return flow — charity location is always known
 *   • On driver cancel: order returns to PENDING so another driver can take it
 *   • On user cancel (after driver): no financial penalty
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository            orderRepository;
    private final OrderMapper                orderMapper;
    private final UserRepository             userRepository;
    private final CharityRepository          charityRepository;
    private final CostCalculationService     costCalculationService;
    private final CommissionSettingsService  commissionSettingsService;
    private final WalletService              walletService;
    private final OrderOtpService            otpService;
    private final StorageService             storageService;
    private final OrderFinanceService        financeService;
    private final ApplicationEventPublisher  eventPublisher;

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
    //  1. Create — REGULAR
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
        double appCommission  = financeService.calculateCommission(deliveryCost, commissionRate);
        double driverEarnings = financeService.calculateDriverEarnings(deliveryCost, appCommission);

        Wallet userWallet = walletService.getWalletByUserId(userId);
        double required = financeService.calculateFreezeAmount(deliveryCost);
        if (userWallet.getWalletBalance() < required)
            throw new BusinessException("Insufficient balance. Need at least " + required + " EGP.");

        Order order = orderMapper.toEntity(form);
        order.setOrderCategory(OrderCategory.REGULAR);
        order.setUserId(userId);
        order.setDeliveryCost(deliveryCost);
        order.setDistanceKm(cost.getDistanceKm());
        order.setCommissionRate(commissionRate);
        order.setAppCommission(appCommission);
        order.setDriverEarnings(driverEarnings);
        order.setTrackingNumber(generateTrackingNumber());
        order.setCreationStatus(CreationStatus.CREATED);
        order.setStatus(OrderStatus.PENDING);
        order.setPhoto(storageService.savePhoto(form.getPhoto()));

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  1-B. Create — CHARITY (WasalElkheer)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Creates a charity donation order.
     *
     * Differences from REGULAR:
     *  - Destination is a known Charity — coordinates fetched from CharityRepository.
     *  - No wallet balance pre-check — user donates items, not money.
     *  - No commission — app pays driver in full on delivery.
     *  - Uses pickupLatitude/pickupLongitude for the donor's location (reuses the
     *    same Order fields, avoiding a separate table).
     */
    public CreationDto createCharityOrder(CharityOrderForm form, UUID userId) throws IOException {
        User user = getActiveUser(userId);
        requireUser(user);

        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found: " + form.getCharityId()));
        if (!charity.isActive()) throw new BusinessException("Charity is not active");

        // Distance: donor location → charity location
        PricingDetails cost = costCalculationService.calculateCost(
                form.getUserLatitude(), form.getUserLongitude(),
                charity.getLatitude(), charity.getLongitude(),
                null);

        Order order = new Order();
        order.setOrderCategory(OrderCategory.CHARITY);
        order.setUserId(userId);
        order.setCharityId(form.getCharityId());

        // Donor location stored in pickup fields for consistency
        order.setPickupLatitude(form.getUserLatitude());
        order.setPickupLongitude(form.getUserLongitude());
        order.setPickupAddress(form.getAddress());

        // Charity location stored in recipient fields
        order.setRecipientLatitude(charity.getLatitude());
        order.setRecipientLongitude(charity.getLongitude());
        order.setRecipientAddress(charity.getNameEn());
        order.setRecipientName(charity.getNameEn());
        order.setRecipientPhone(charity.getPhone());

        order.setOrderType(form.getOrderType());
        order.setAdditionalNotes(form.getAdditionalNotes());
        order.setCollectionDate(form.getCollectionDate());
        order.setAnyTime(form.isAnyTime());
        order.setAllowInspection(form.isAllowInspection());
        order.setReceiverPaysShipping(false); // app always pays for charity orders

        order.setDeliveryCost(cost.getTotalCost());
        order.setDistanceKm(cost.getDistanceKm());

        // Commission fields stay 0.0 — no app commission on donations
        order.setCommissionRate(0.0);
        order.setAppCommission(0.0);
        order.setDriverEarnings(cost.getTotalCost()); // driver earns full amount

        order.setTrackingNumber(generateTrackingNumber());
        order.setCreationStatus(CreationStatus.CREATED);
        order.setStatus(OrderStatus.PENDING);
        order.setPhoto(storageService.savePhoto(form.getPhoto()));

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  2. Confirm Order
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR: freezes user wallet (2× deliveryCost).
     * CHARITY: no freeze — user donates items, not money.
     */
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

        if (order.getOrderCategory() == OrderCategory.REGULAR) {
            financeService.freezeForOrder(userId, order.getDeliveryCost());
        }
        // CHARITY: no freeze

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.ORDER_CONFIRMED, null, pickupOtp);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  3. Update Order
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR: recalculates cost and swaps the frozen wallet amount.
     * CHARITY: recalculates cost (user→charity) but no wallet operation.
     */
    public CreationDto updateOrder(Long orderId, OrderForm form, UUID userId) throws IOException {
        User actor = getActiveUser(userId);
        if (!actor.isUser() && !actor.isAdmin())
            throw new BusinessException("Users or admins only");

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId) && !actor.isAdmin())
            throw new BusinessException("You are not allowed to update this order");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Order cannot be updated in current status");

        // CHARITY update uses a separate form path
        if (order.getOrderCategory() == OrderCategory.CHARITY)
            throw new BusinessException(
                    "Use updateCharityOrder() to update a charity order");

        PricingDetails cost = costCalculationService.calculateCost(
                form.getPickupLatitude(), form.getPickupLongitude(),
                form.getRecipientLatitude(), form.getRecipientLongitude(),
                form.getInsuranceValue());

        CommissionSettings commission = commissionSettingsService.getActiveSettings();
        double newDeliveryCost = cost.getTotalCost();
        double commissionRate  = commission.getCommissionRate();
        double appCommission   = financeService.calculateCommission(newDeliveryCost, commissionRate);
        double driverEarnings  = financeService.calculateDriverEarnings(newDeliveryCost, appCommission);

        financeService.refreezeForOrder(order.getUserId(), order.getDeliveryCost(), newDeliveryCost);

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
        order.setDriverEarnings(driverEarnings);
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

    /**
     * Update a CHARITY order — recalculates cost with updated location/charity.
     * No wallet refreeze because no money was taken from the user.
     */
    public CreationDto updateCharityOrder(Long orderId, CharityOrderForm form, UUID userId) throws IOException {
        User actor = getActiveUser(userId);
        if (!actor.isUser() && !actor.isAdmin())
            throw new BusinessException("Users or admins only");

        Order order = findOrder(orderId);

        if (order.getOrderCategory() != OrderCategory.CHARITY)
            throw new BusinessException("This order is not a charity order");
        if (!order.getUserId().equals(userId) && !actor.isAdmin())
            throw new BusinessException("You are not allowed to update this order");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Order cannot be updated in current status");

        Charity charity = charityRepository.findById(form.getCharityId())
                .orElseThrow(() -> new ResourceNotFoundException("Charity not found: " + form.getCharityId()));
        if (!charity.isActive()) throw new BusinessException("Charity is not active");

        PricingDetails cost = costCalculationService.calculateCost(
                form.getUserLatitude(), form.getUserLongitude(),
                charity.getLatitude(), charity.getLongitude(),
                null);

        order.setCharityId(form.getCharityId());
        order.setPickupLatitude(form.getUserLatitude());
        order.setPickupLongitude(form.getUserLongitude());
        order.setPickupAddress(form.getAddress());
        order.setRecipientLatitude(charity.getLatitude());
        order.setRecipientLongitude(charity.getLongitude());
        order.setRecipientAddress(charity.getNameEn());
        order.setRecipientName(charity.getNameEn());
        order.setRecipientPhone(charity.getPhone());
        order.setOrderType(form.getOrderType());
        order.setAdditionalNotes(form.getAdditionalNotes());
        order.setCollectionDate(form.getCollectionDate());
        order.setAnyTime(form.isAnyTime());
        order.setAllowInspection(form.isAllowInspection());
        order.setDeliveryCost(cost.getTotalCost());
        order.setDistanceKm(cost.getDistanceKm());
        order.setDriverEarnings(cost.getTotalCost()); // driver earns full amount

        String newPhoto = storageService.savePhoto(form.getPhoto());
        if (newPhoto != null) order.setPhoto(newPhoto);

        return enrichCreationDto(orderMapper.toCreationDto(orderRepository.save(order)));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  4. Driver Accepts
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR: checks driver wallet covers insurance value before accepting.
     * CHARITY: no insurance balance check — driver has no financial liability.
     */
    public OrderDto driverConfirmOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getCreationStatus() != CreationStatus.CONFIRMED)
            throw new BusinessException("Order not yet confirmed by the user");
        if (order.getDriverId() != null)
            throw new BusinessException("Order already taken by another driver");

        if (order.getOrderCategory() == OrderCategory.REGULAR) {
            Wallet driverWallet = walletService.getWalletByUserId(driverId);
            if (driverWallet.getWalletBalance() < order.getInsuranceValue())
                throw new BusinessException("You Don't have enough balance. Need at least "
                        + order.getInsuranceValue() + " EGP to accept this order.");
        }
        // CHARITY: no insurance balance check

        order.setDriverId(driverId);
        order.setStatus(OrderStatus.ACCEPTED);
        order.setConfirmedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.DRIVER_ACCEPTED, driverId, null);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  5. Confirm Pickup
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR: freezes driver's insurance as collateral.
     *           Delivery OTP is sent to the recipient's phone.
     * CHARITY:  no insurance freeze.
     *           Delivery OTP is sent to the charity's phone.
     *           (The charity phone is already stored in recipientPhone.)
     */
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
        order.setOtpForPickup(null); // clear used OTP

        if (order.getOrderCategory() == OrderCategory.REGULAR) {
            financeService.freezeDriverInsurance(driverId, order.getInsuranceValue());
        }
        // CHARITY: no insurance freeze — delivery OTP recipient is charity phone
        // (stored in recipientPhone), handled by the notification event below

        Order saved = orderRepository.save(order);
        publish(saved, OrderNotificationEvent.Type.DELIVERY_OTP_SENT, null, deliveryOtp);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  6. Confirm Delivery
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR: settleDelivery — user pays deliveryCost, driver earns net,
     *          driver insurance unfrozen, user frozen amount unfrozen.
     * CHARITY: app credits driver the full deliveryCost.
     *          No deduction from user wallet. No freeze to undo.
     */
    public OrderDto confirmDelivery(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can confirm delivery");
        if (order.getStatus() != OrderStatus.IN_PROGRESS)
            throw new BusinessException("Order must be IN_PROGRESS or IN_THE_WAY");
        if (!otp.equals(order.getOtpForDelivery()))
            throw new BusinessException("Invalid delivery OTP");

        order.setOtpForDelivery(null); // clear used OTP
        order.setDeliveryConfirmed(true);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        if (order.getOrderCategory() == OrderCategory.REGULAR) {
            financeService.settleDelivery(saved);
        } else {
            // CHARITY: app pays driver full deliveryCost
            if (saved.getDeliveryCost() > 0) {
                Wallet driverWallet = walletService.getWalletByUserId(saved.getDriverId());
                driverWallet.setWalletBalance(driverWallet.getWalletBalance() + saved.getDeliveryCost());
                walletService.save(driverWallet);
            }
        }

        publish(saved, OrderNotificationEvent.Type.DELIVERY_CONFIRMED, null, null);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7-A. Return — REGULAR only
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto returnOrder(Long orderId, UUID driverId) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (order.getOrderCategory() == OrderCategory.CHARITY)
            throw new BusinessException("Charity orders do not support return flow");

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
        publish(saved, OrderNotificationEvent.Type.RETURN_INITIATED, null, returnOtp);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  7-B. Confirm Return — REGULAR only
    // ═══════════════════════════════════════════════════════════════════════

    public OrderDto confirmReturn(Long orderId, UUID driverId, String otp) {
        User driver = getActiveUser(driverId);
        requireDriver(driver);

        Order order = findOrder(orderId);

        if (order.getOrderCategory() == OrderCategory.CHARITY)
            throw new BusinessException("Charity orders do not support return flow");

        if (!order.getDriverId().equals(driverId))
            throw new BusinessException("Only the assigned driver can confirm return");
        if (order.getStatus() != OrderStatus.IN_RETURN)
            throw new BusinessException("Order must be IN_RETURN");
        if (order.getOtpForReturn() == null || !order.getOtpForReturn().equals(otp))
            throw new BusinessException("Invalid return OTP");

        order.setStatus(OrderStatus.RETURNED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        financeService.settleReturn(saved);
        publish(saved, OrderNotificationEvent.Type.RETURN_CONFIRMED, null, null);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  8. Driver Cancels
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Both REGULAR and CHARITY: resets order to PENDING so another driver can accept it.
     * The difference is only in what gets reset on the wallet side — for REGULAR, nothing
     * needs undoing at ACCEPTED stage (driver insurance is only frozen at PICKUP, not here).
     */
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
        publish(saved, OrderNotificationEvent.Type.DRIVER_CANCELLED, driverId, null);
        return enrichDto(orderMapper.toDto(saved));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  9. User Cancels
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * REGULAR:
     *   - No driver: unfreeze user wallet.
     *   - Driver assigned (ACCEPTED): penalty = 1× deliveryCost paid to driver.
     *   - Cannot cancel at IN_RETURN or DELIVERED.
     *
     * CHARITY:
     *   - No driver: nothing to undo (no money was frozen).
     *   - Driver assigned (ACCEPTED): no penalty — no money was taken from user.
     *   - Cannot cancel at IN_PROGRESS, IN_THE_WAY, or DELIVERED (items already collected).
     */
    public OrderDto cancelOrderByUser(Long orderId, UUID userId, String reason) {
        User user = getActiveUser(userId);
        if (!user.isUser() && !user.isAdmin())
            throw new BusinessException("Users or admins only");

        Order order = findOrder(orderId);

        if (!order.getUserId().equals(userId) && !user.isAdmin())
            throw new BusinessException("Not authorized to cancel this order");
        if (order.getStatus() == OrderStatus.CANCELLED)
            throw new BusinessException("Order already cancelled");

        if (order.getOrderCategory() == OrderCategory.REGULAR) {
            if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.IN_RETURN)
                throw new BusinessException("Cannot cancel at stage: " + order.getStatus());

            if (order.getDriverId() == null) {
                financeService.unfreezeForCancel(order.getUserId(), order.getDeliveryCost());
                order.setStatus(OrderStatus.CANCELLED);
                order.setRejectionReason(reason);
                Order saved = orderRepository.save(order);
                publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_NO_DRIVER, null, reason);
                return enrichDto(orderMapper.toDto(saved));
            }

            financeService.settleCancellationWithPenalty(order);
            order.setStatus(OrderStatus.CANCELLED);
            order.setRejectionReason(reason != null ? reason : "Cancelled by user after driver acceptance");
            Order saved = orderRepository.save(order);
            publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_WITH_DRIVER, null, null);
            return enrichDto(orderMapper.toDto(saved));

        } else {
            // CHARITY
            if (order.getStatus() == OrderStatus.IN_PROGRESS
                    || order.getStatus() == OrderStatus.DELIVERED)
                throw new BusinessException("Cannot cancel after items have been collected: " + order.getStatus());

            UUID assignedDriverId = order.getDriverId();
            order.setStatus(OrderStatus.CANCELLED);
            order.setDriverId(null);
            order.setRejectionReason(reason);
            Order saved = orderRepository.save(order);

            if (assignedDriverId != null) {
                // notify driver — no penalty, just inform
                publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_WITH_DRIVER, assignedDriverId, reason);
            } else {
                publish(saved, OrderNotificationEvent.Type.USER_CANCELLED_NO_DRIVER, null, reason);
            }
            return enrichDto(orderMapper.toDto(saved));
        }
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

    /** Filter by category so each app screen only sees its own order type. */
    public List<OrderDto> getAvailableOrdersByCategory(UUID driverId, OrderCategory category) {
        requireDriver(getActiveUser(driverId));
        return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING)
                .stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED
                        && o.getOrderCategory() == category)
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

    public List<OrderDto> getAllOrders(UUID adminId) {
        requireAdmin(getActiveUser(adminId));
        return orderRepository.findAll().stream()
                .filter(o -> o.getCreationStatus() == CreationStatus.CONFIRMED)
                .map(orderMapper::toDto).map(this::enrichDto).collect(Collectors.toList());
    }

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
        dto.setPending(   count(orders, OrderStatus.PENDING));
        dto.setAccepted(  count(orders, OrderStatus.ACCEPTED));
        dto.setInProgress(count(orders, OrderStatus.IN_PROGRESS));
        dto.setInDelivery(count(orders, OrderStatus.RETURNED));
        dto.setDelivered( count(orders, OrderStatus.DELIVERED));
        dto.setCancelled( count(orders, OrderStatus.CANCELLED));
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

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