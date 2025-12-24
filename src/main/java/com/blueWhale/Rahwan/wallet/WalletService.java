// ============================================
// WalletService.java (Updated)
// ============================================
package com.blueWhale.Rahwan.wallet;

import com.blueWhale.Rahwan.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;

    public Wallet createWalletForUser(User user){

        return walletRepository.findByUserId(user.getId())
                .orElseGet(()-> {
                    Wallet wallet = new Wallet();
                    wallet.setUser(user);
                    wallet.setBalance(0.0);
                    wallet.setFrozenBalance(0.0);
                    wallet.setCreatedAt(LocalDateTime.now());

                    return walletRepository.save(wallet);
                });
    }
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found for user: " + userId));
    }

    /**
     * تجميد مبلغ من الرصيد
     */
    public void freezeAmount(Wallet wallet, double amount) {
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: " + wallet.getBalance() + ", Required: " + amount);
        }

        wallet.setBalance(wallet.getBalance() - amount);
        wallet.setFrozenBalance(wallet.getFrozenBalance() + amount);
        walletRepository.save(wallet);
    }

    /**
     * فك تجميد مبلغ (إرجاع للرصيد)
     */
    public void unfreezeAmount(Wallet wallet, double amount) {
        if (wallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance");
        }

        wallet.setFrozenBalance(wallet.getFrozenBalance() - amount);
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
    }

    /**
     * تحويل مبلغ مجمد من محفظة لمحفظة
     */
    public void transferFrozenAmount(UUID fromUserId, UUID toUserId, double amount) {
        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUserId);

        if (fromWallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance in source wallet");
        }

        // خصم من المجمد
        fromWallet.setFrozenBalance(fromWallet.getFrozenBalance() - amount);

        // إضافة للرصيد العادي في المحفظة الثانية
        toWallet.setBalance(toWallet.getBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    public Wallet updateUserBalance(UUID userId, double newBalance) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setBalance(newBalance);
        return walletRepository.save(wallet);
    }
}