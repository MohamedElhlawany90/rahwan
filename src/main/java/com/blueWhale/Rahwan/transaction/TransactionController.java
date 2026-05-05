package com.blueWhale.Rahwan.transaction;

import com.blueWhale.Rahwan.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Wallet history — all real money movements (sent + received) for the current user.
     * GET /api/transactions/my-history
     */
    @GetMapping("/my-history")
    public ResponseEntity<List<TransactionDto>> getMyWalletHistory(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getWalletHistory(principal.getId()));
    }

    /**
     * Admin: wallet history for any user by ID.
     * GET /api/transactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TransactionDto>> getUserWalletHistory(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(transactionService.getWalletHistory(userId));
    }

    /**
     * All transactions for a specific order.
     * GET /api/transactions/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TransactionDto>> getOrderTransactions(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(transactionService.getTransactionsByOrder(orderId));
    }

    /**
     * Outgoing (debit) transactions only for the current user.
     * GET /api/transactions/my-outgoing
     */
    @GetMapping("/my-outgoing")
    public ResponseEntity<List<TransactionDto>> getMyOutgoing(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getOutgoingTransactions(principal.getId()));
    }

    /**
     * Incoming (credit) transactions only for the current user.
     * GET /api/transactions/my-incoming
     */
    @GetMapping("/my-incoming")
    public ResponseEntity<List<TransactionDto>> getMyIncoming(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionService.getIncomingTransactions(principal.getId()));
    }
}