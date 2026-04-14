package com.blueWhale.Rahwan.wallet;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.user.User;
import lombok.Getter;
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

    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
    }

    public WalletDto getWalletDtoByUserId(UUID userId) {
        Wallet wallet = getWalletByUserId(userId);
        return walletMapper.toDto(wallet);
    }

    public WalletDto addBalance(UUID userId, double amount) {
        if (amount <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        Wallet updated = walletRepository.save(wallet);
        return walletMapper.toDto(updated);
    }

    public void transferBalance(UUID fromUserId, UUID toUserId, double amount) {
        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUserId);

        if (fromWallet.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: " +
                    fromWallet.getWalletBalance() + ", Required: " + amount);
        }

        fromWallet.setWalletBalance(fromWallet.getWalletBalance() - amount);
        toWallet.setWalletBalance(toWallet.getWalletBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    public void checkHasEnoughBalance(UUID userId, double amount) {
        Wallet wallet = getWalletByUserId(userId);
        if (wallet.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: " +
                    wallet.getWalletBalance() + ", Required: " + amount);
        }
    }



    public void freezeAmount(Wallet wallet, double amount) {
        if (wallet.getWalletBalance() < amount) {
            throw new RuntimeException("Insufficient balance. Available: " +
                    wallet.getWalletBalance() + ", Required: " + amount);
        }
        wallet.setWalletBalance(wallet.getWalletBalance() - amount);
        wallet.setFrozenBalance(wallet.getFrozenBalance() + amount);
        walletRepository.save(wallet);
    }

    public WalletDto freezeAmountByUserId(UUID userId, double amount) {
        Wallet wallet = getWalletByUserId(userId);
        freezeAmount(wallet, amount);
        return walletMapper.toDto(wallet);
    }

    public void unfreezeAmount(Wallet wallet, double amount) {
        if (wallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance. Available: " +
                    wallet.getFrozenBalance() + ", Required: " + amount);
        }
        wallet.setFrozenBalance(wallet.getFrozenBalance() - amount);
        wallet.setWalletBalance(wallet.getWalletBalance() + amount);
        walletRepository.save(wallet);
    }

    public WalletDto unfreezeAmountByUserId(UUID userId, double amount) {
        Wallet wallet = getWalletByUserId(userId);
        unfreezeAmount(wallet, amount);
        return walletMapper.toDto(wallet);
    }

    public void transferFrozenAmount(UUID fromUserId, UUID toUserId, double amount) {
        Wallet fromWallet = getWalletByUserId(fromUserId);
        Wallet toWallet = getWalletByUserId(toUserId);

        if (fromWallet.getFrozenBalance() < amount) {
            throw new RuntimeException("Insufficient frozen balance in source wallet. Available: " +
                    fromWallet.getFrozenBalance() + ", Required: " + amount);
        }

        fromWallet.setFrozenBalance(fromWallet.getFrozenBalance() - amount);
        toWallet.setWalletBalance(toWallet.getWalletBalance() + amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
    }

    @Deprecated
    public Wallet updateUserBalance(UUID userId, double addedBalance) {
        Wallet wallet = getWalletByUserId(userId);
        wallet.setWalletBalance(wallet.getWalletBalance() + addedBalance);
        return walletRepository.save(wallet);
    }
    public void save (Wallet wallet) {
        walletRepository.save(wallet);
    }

}