//package com.blueWhale.Rahwan.wallet;
//
//import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/wallets")
//@RequiredArgsConstructor
//public class WalletController {
//
//    private final WalletService walletService;
//
//    @GetMapping("/user/{userId}")
//    public ResponseEntity<WalletDto> getUserWallet(@PathVariable UUID userId) {
//        try {
//            WalletDto wallet = walletService.getWalletDtoByUserId(userId);
//            return ResponseEntity.ok(wallet);
//        } catch (ResourceNotFoundException ex) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        }
//    }
//
//    @PostMapping("/user/{userId}/add-balance")
//    public ResponseEntity<WalletDto> addBalance(
//            @PathVariable UUID userId,
//            @RequestParam double amount) {
//        try {
//            if (amount <= 0) {
//                return ResponseEntity.badRequest().body(null);
//            }
//            WalletDto updated = walletService.addBalance(userId, amount);
//            return ResponseEntity.ok(updated);
//        } catch (ResourceNotFoundException ex) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//        } catch (RuntimeException ex) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//        }
//    }
//
//    @PostMapping("/user/{userId}/freeze")
//    public ResponseEntity<WalletDto> freezeAmount(
//            @PathVariable UUID userId,
//            @RequestParam double amount) {
//        try {
//            if (amount <= 0) {
//                return ResponseEntity.badRequest().body(null);
//            }
//            WalletDto updated = walletService.freezeAmountByUserId(userId, amount);
//            return ResponseEntity.ok(updated);
//        } catch (RuntimeException ex) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//        }
//    }
//
//    @PostMapping("/user/{userId}/unfreeze")
//    public ResponseEntity<WalletDto> unfreezeAmount(
//            @PathVariable UUID userId,
//            @RequestParam double amount) {
//        try {
//            if (amount <= 0) {
//                return ResponseEntity.badRequest().body(null);
//            }
//            WalletDto updated = walletService.unfreezeAmountByUserId(userId, amount);
//            return ResponseEntity.ok(updated);
//        } catch (RuntimeException ex) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
//        }
//    }
//
//    @PostMapping("/transfer")
//    public ResponseEntity<String> transferFrozen(
//            @RequestParam UUID fromUserId,
//            @RequestParam UUID toUserId,
//            @RequestParam double amount) {
//        try {
//            if (amount <= 0) {
//                return ResponseEntity.badRequest().body("Amount must be positive");
//            }
//            walletService.transferFrozenAmount(fromUserId, toUserId, amount);
//            return ResponseEntity.ok("Transfer successful");
//        } catch (RuntimeException ex) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
//        }
//    }
//}