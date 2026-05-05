package com.blueWhale.Rahwan.transaction;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserRepository userRepository;

    // ==================== Logging (called from OrderService) ====================

    /**
     * confirmDelivery — driver pays user (insuranceValue − appCommission);
     * platform keeps appCommission.
     *
     * @param orderId        order that was delivered
     * @param trackingNumber human-readable reference
     * @param fromUserId     driver's UUID  (money leaves driver wallet)
     * @param toUserId       user's UUID    (money enters user wallet)
     * @param amount         net amount credited to the user  (insuranceValue − appCommission)
     * @param appCommission  platform fee kept (appCommission)
     */
    public Transaction logDeliveryCompleted(
            Long orderId,
            String trackingNumber,
            UUID fromUserId,
            UUID toUserId,
            double amount,
            double appCommission
    ) {
        String description = String.format(
                "Delivery completed. Driver paid %.2f EGP to sender (platform commission: %.2f EGP).",
                amount, appCommission
        );
        return save(orderId, trackingNumber, fromUserId, toUserId,
                amount, appCommission, TransactionType.DELIVERY_COMPLETED, description);
    }

    /**
     * confirmReturn — user pays driver a return penalty of (deliveryCost × 2).
     *
     * @param fromUserId user's UUID    (money leaves user wallet)
     * @param toUserId   driver's UUID  (money enters driver wallet)
     * @param amount     returnPenalty  (deliveryCost × 2)
     */
    public Transaction logReturnPenalty(
            Long orderId,
            String trackingNumber,
            UUID fromUserId,
            UUID toUserId,
            double amount
    ) {
        String description = String.format(
                "Order returned. Sender charged %.2f EGP (2× delivery cost) as a return penalty paid to driver.",
                amount
        );
        return save(orderId, trackingNumber, fromUserId, toUserId,
                amount, 0.0, TransactionType.RETURN_PENALTY, description);
    }

    /**
     * cancelOrderByUser after driver accepted — user pays driver (deliveryCost) as compensation.
     *
     * @param fromUserId user's UUID    (money leaves user wallet)
     * @param toUserId   driver's UUID  (money enters driver wallet)
     * @param amount     compensation   (deliveryCost)
     */
    public Transaction logCancellationCompensation(
            Long orderId,
            String trackingNumber,
            UUID fromUserId,
            UUID toUserId,
            double amount
    ) {
        String description = String.format(
                "Order cancelled after driver acceptance. Sender paid %.2f EGP as compensation to driver.",
                amount
        );
        return save(orderId, trackingNumber, fromUserId, toUserId,
                amount, 0.0, TransactionType.CANCELLATION_COMPENSATION, description);
    }

    // ==================== Queries ====================

    /**
     * Full wallet history for a user: all transactions where they sent OR received money.
     * Names are enriched from UserRepository.
     */
    @Transactional(readOnly = true)
    public List<TransactionDto> getWalletHistory(UUID userId) {
        // validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return transactionRepository.findWalletHistory(userId)
                .stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    /** All transactions recorded for a specific order. */
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByOrder(Long orderId) {
        return transactionRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    /** Outgoing (debit) transactions for a user. */
    @Transactional(readOnly = true)
    public List<TransactionDto> getOutgoingTransactions(UUID userId) {
        return transactionRepository.findByFromUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    /** Incoming (credit) transactions for a user. */
    @Transactional(readOnly = true)
    public List<TransactionDto> getIncomingTransactions(UUID userId) {
        return transactionRepository.findByToUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::enrich)
                .collect(Collectors.toList());
    }

    // ==================== Private Helpers ====================

    private Transaction save(
            Long orderId,
            String trackingNumber,
            UUID fromUserId,
            UUID toUserId,
            double amount,
            double appCommission,
            TransactionType type,
            String description
    ) {
        Transaction tx = Transaction.builder()
                .orderId(orderId)
                .trackingNumber(trackingNumber)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .appCommission(appCommission)
                .type(type)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        return transactionRepository.save(tx);
    }

    /** Attaches user display names to the DTO. */
    private TransactionDto enrich(Transaction tx) {
        TransactionDto dto = transactionMapper.toDto(tx);
        userRepository.findById(tx.getFromUserId())
                .ifPresent(u -> dto.setFromUserName(u.getName()));
        userRepository.findById(tx.getToUserId())
                .ifPresent(u -> dto.setToUserName(u.getName()));
        return dto;
    }
}