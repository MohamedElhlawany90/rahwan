package com.blueWhale.Rahwan.order;

import com.blueWhale.Rahwan.exception.BusinessException;
import com.blueWhale.Rahwan.transaction.TransactionService;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Responsible for one thing only: all money movements related to orders.
 *
 * Central rule: whenever you need to change how money flows
 * (e.g. change the freeze multiplier from 2x to 1.5x),
 * you change it in exactly ONE place — here.
 */
@Service
@RequiredArgsConstructor
public class OrderFinanceService {

    /**
     * The multiplier used when freezing the user's wallet at order confirmation.
     * 2x = one for delivery cost + one as a security deposit.
     * Change this constant to adjust the freeze amount globally.
     */
    private static final double FREEZE_MULTIPLIER = 2.0;

    private final WalletService walletService;
    private final TransactionService transactionService;

    // ─── Calculations ─────────────────────────────────────────────────────

    /**
     * ✅ FIX: Commission is now calculated on deliveryCost, not insuranceValue.
     * Example: deliveryCost=100, rate=10% → appCommission=10, driverEarnings=90
     */
    public double calculateCommission(double deliveryCost, double commissionRate) {
        return round((deliveryCost * commissionRate) / 100.0);
    }

    /**
     * ✅ FIX: Driver earnings = what the driver actually receives after platform fee.
     */
    public double calculateDriverEarnings(double deliveryCost, double appCommission) {
        return round(deliveryCost - appCommission);
    }

    public double calculateFreezeAmount(double deliveryCost) {
        return round(deliveryCost * FREEZE_MULTIPLIER);
    }

    // ─── Freeze / Unfreeze ────────────────────────────────────────────────

    /** confirmOrder: freeze user's wallet (deliveryCost × 2). */
    public void freezeForOrder(UUID userId, double deliveryCost) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        walletService.freezeAmount(wallet, calculateFreezeAmount(deliveryCost));
    }

    /** updateOrder: swap old frozen amount with new one. */
    public void refreezeForOrder(UUID userId, double oldDeliveryCost, double newDeliveryCost) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        walletService.unfreezeAmount(wallet, calculateFreezeAmount(oldDeliveryCost));
        walletService.freezeAmount(wallet, calculateFreezeAmount(newDeliveryCost));
    }

    /** confirmPickup: freeze driver's insurance as collateral. */
    public void freezeDriverInsurance(UUID driverId, double insuranceValue) {
        Wallet wallet = walletService.getWalletByUserId(driverId);
        walletService.freezeAmount(wallet, insuranceValue);
    }

    /** cancelByUser (no driver): return the frozen amount to user. */
    public void unfreezeForCancel(UUID userId, double deliveryCost) {
        Wallet wallet = walletService.getWalletByUserId(userId);
        walletService.unfreezeAmount(wallet, calculateFreezeAmount(deliveryCost));
    }

    // ─── Settlement ───────────────────────────────────────────────────────

    /**
     * confirmDelivery — settle all money between user, driver, and platform.
     *
     * Flow:
     *  1. Unfreeze user's 2x deliveryCost → back to liquid
     *  2. Unfreeze driver's insurance → back to liquid
     *  3. User pays deliveryCost → user balance -= deliveryCost
     *  4. Driver earns (deliveryCost - appCommission) → driver balance += driverEarnings
     *  5. Platform keeps appCommission implicitly (difference between 3 and 4)
     */
    public void settleDelivery(Order order) {
        Wallet userWallet   = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        // 1. Unfreeze both sides
        walletService.unfreezeAmount(userWallet,   calculateFreezeAmount(order.getDeliveryCost()));
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        // 2. User pays delivery cost
        userWallet.setWalletBalance(userWallet.getWalletBalance() - order.getDeliveryCost());

        // 3. Driver earns the net amount (already persisted on the order entity)
        driverWallet.setWalletBalance(driverWallet.getWalletBalance() + order.getDriverEarnings());

        walletService.save(userWallet);
        walletService.save(driverWallet);

        // 4. Log transaction
        transactionService.logDeliveryCompleted(
                order.getId(),
                order.getTrackingNumber(),
                order.getUserId(),
                order.getDriverId(),
                order.getDriverEarnings(),
                order.getAppCommission()
        );
    }

    /**
     * confirmReturn — user pays driver (deliveryCost × 2) as a return penalty.
     *
     * Flow:
     *  1. Unfreeze user's 2x → back to liquid
     *  2. Unfreeze driver's insurance → back to liquid
     *  3. User pays return penalty (2x deliveryCost) → user balance -= penalty
     *  4. Driver receives return penalty → driver balance += penalty
     */
    public void settleReturn(Order order) {
        Wallet userWallet   = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        double penalty = calculateFreezeAmount(order.getDeliveryCost()); // deliveryCost × 2

        walletService.unfreezeAmount(userWallet,   penalty);
        walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        if (userWallet.getWalletBalance() < penalty)
            throw new BusinessException("Insufficient balance to cover return penalty: " + penalty + " EGP");

        userWallet.setWalletBalance(userWallet.getWalletBalance() - penalty);
        driverWallet.setWalletBalance(driverWallet.getWalletBalance() + penalty);

        walletService.save(userWallet);
        walletService.save(driverWallet);

        transactionService.logReturnPenalty(
                order.getId(),
                order.getTrackingNumber(),
                order.getUserId(),
                order.getDriverId(),
                penalty
        );
    }

    /**
     * cancelOrderByUser (after driver accepted) — user pays driver deliveryCost as compensation.
     *
     * Flow:
     *  1. Unfreeze user's 2x → back to liquid
     *  2. If IN_PROGRESS: also unfreeze driver's insurance
     *  3. User pays compensation (1x deliveryCost) → user balance -= compensation
     *  4. Driver receives compensation → driver balance += compensation
     */
    public void settleCancellationWithPenalty(Order order) {
        Wallet userWallet   = walletService.getWalletByUserId(order.getUserId());
        Wallet driverWallet = walletService.getWalletByUserId(order.getDriverId());

        walletService.unfreezeAmount(userWallet, calculateFreezeAmount(order.getDeliveryCost()));

        if (order.getStatus() == OrderStatus.IN_PROGRESS)
            walletService.unfreezeAmount(driverWallet, order.getInsuranceValue());

        double compensation = order.getDeliveryCost();

        if (userWallet.getWalletBalance() < compensation)
            throw new BusinessException("Insufficient balance for cancellation penalty. Required: "
                    + compensation + " EGP, Available: " + userWallet.getWalletBalance() + " EGP");

        userWallet.setWalletBalance(userWallet.getWalletBalance() - compensation);
        driverWallet.setWalletBalance(driverWallet.getWalletBalance() + compensation);

        walletService.save(userWallet);
        walletService.save(driverWallet);

        transactionService.logCancellationCompensation(
                order.getId(),
                order.getTrackingNumber(),
                order.getUserId(),
                order.getDriverId(),
                compensation
        );
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}