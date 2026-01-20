package com.blueWhale.Rahwan.wallet;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
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
    private final WalletMapper walletMapper;

    /**
     * إنشاء محفظة جديدة للمستخدم
     */
    public Wallet createWalletForUser(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .walletBalance(0.0)
                            .frozenBalance(0.0)
                            .createdAt(LocalDateTime.now())
                            .build();

                    user.setWallet(wallet);
                    return walletRepository.save(wallet);
                });
    }

    /**
     * جلب المحفظة بـ Entity
     */
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    /**
     * جلب المحفظة بـ DTO
     */
    public WalletDto getWalletDtoByUserId(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        return walletMapper.toDto(wallet);
    }

    /**
     * إضافة رصيد للمحفظة
     */
    public WalletDto addBalance(UUID userId, double amount) {
        if (amount <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        Wallet updated = walletRepository.save(wallet);

        return walletMapper.toDto(updated);
    }

    /**
     * تجميد مبلغ من الرصيد
     */
    public void freezeAmount(Wallet wallet, double amount) {
        if (wallet.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: " +
                    wallet.getWalletBalance() + ", Required: " + amount);
        }

        wallet.setWalletBalance(wallet.getWalletBalance() - amount);
        wallet.setFrozenBalance(wallet.getFrozenBalance() + amount);
        walletRepository.save(wallet);
    }

    /**
     * تجميد مبلغ من الرصيد بـ userId
     */
    public WalletDto freezeAmountByUserId(UUID userId, double amount) {
        Wallet wallet = getWalletByUserId(userId);
        freezeAmount(wallet, amount);
        return walletMapper.toDto(wallet);
    }

    /**
     * فك تجميد مبلغ (إرجاع للرصيد)
     */
    public void unfreezeAmount(Wallet wallet, double amount) {
        if (wallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance. Available: " +
                    wallet.getFrozenBalance() + ", Required: " + amount);
        }

        wallet.setFrozenBalance(wallet.getFrozenBalance() - amount);
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        walletRepository.save(wallet);
    }

    /**
     * فك تجميد مبلغ بـ userId
     */
    public WalletDto unfreezeAmountByUserId(UUID userId, double amount) {
        Wallet wallet = getWalletByUserId(userId);
        unfreezeAmount(wallet, amount);
        return walletMapper.toDto(wallet);
    }

    /**
     * تحويل مبلغ مجمد من محفظة لمحفظة
     */
    public void transferFrozenAmount(UUID fromUserId, UUID toUserId, double amount) {
        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUserId);

        if (fromWallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance in source wallet. Available: " +
                    fromWallet.getFrozenBalance() + ", Required: " + amount);
        }

        // خصم من المجمد
        fromWallet.setFrozenBalance(fromWallet.getFrozenBalance() - amount);

        // إضافة للرصيد العادي في المحفظة الثانية
        toWallet.setWalletBalance(toWallet.getWalletBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    /**
     * @deprecated Use addBalance instead
     */
    @Deprecated
    public Wallet updateUserBalance(UUID userId, double addedBalance) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletBalance(wallet.getWalletBalance() + addedBalance);
        return walletRepository.save(wallet);
    }
}