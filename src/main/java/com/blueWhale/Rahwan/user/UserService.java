package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.otp.UserOtpService;
import com.blueWhale.Rahwan.security.jwt.JwtTokenProvider;
import com.blueWhale.Rahwan.util.ImageUtility;
import com.blueWhale.Rahwan.wallet.Wallet;
import com.blueWhale.Rahwan.wallet.WalletDto;
import com.blueWhale.Rahwan.wallet.WalletMapper;
import com.blueWhale.Rahwan.wallet.WalletService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

@Service
@Transactional
public class UserService {

    private static final String UPLOADED_FOLDER = "/home/ubuntu/rahwan/";
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final WalletService walletService;
    private final WalletMapper walletMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserOtpService userOtpService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       WalletService walletService, WalletMapper walletMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       UserOtpService userOtpService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.walletService = walletService;
        this.walletMapper = walletMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userOtpService = userOtpService;
    }

    // ==================== Signup / SignIn ====================

    public UserDto createUser(UserForm form) {
        validatePhone(form.getPhone());

        userRepository.findByPhone(form.getPhone())
                .ifPresent(user -> {
                    throw new RuntimeException("Phone already exists: " + form.getPhone());
                });

        User user = userMapper.toEntity(form);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        Set<UserRole> roles = new HashSet<>();
        roles.add(form.getRole() != null ? form.getRole() : UserRole.user);
        user.setRoles(roles);

        User saved = userRepository.save(user);
        walletService.createWalletForUser(saved);

        return userMapper.toDto(saved);
    }

    /**
     * تسجيل من الـ driver app
     * - لو الرقم موجود: يتحقق من الباسورد ويضيف دور driver
     * - لو الرقم مش موجود: ينشئ أكونت جديد بدور driver
     * - المحفظة مشتركة في الحالتين (نفس الـ UUID)
     */
    public SignInDto signupAsDriver(UserForm form) {
        validatePhone(form.getPhone());

        Optional<User> existing = userRepository.findByPhone(form.getPhone());

        if (existing.isPresent()) {
            // أكونت موجود - نضيف دور driver بعد التحقق من الباسورد
            User user = existing.get();

            if (!passwordEncoder.matches(form.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid password");
            }

            if (user.getRoles().contains(UserRole.driver)) {
                throw new RuntimeException("Account already has driver role");
            }

            user.getRoles().add(UserRole.driver);
            User updated = userRepository.save(user);

            return buildSignInDto(updated);

        } else {
            // أكونت جديد بدور driver فقط
            User user = userMapper.toEntity(form);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            Set<UserRole> roles = new HashSet<>();
            roles.add(UserRole.driver);
            user.setRoles(roles);

            User saved = userRepository.save(user);
            walletService.createWalletForUser(saved);

            return buildSignInDto(saved);
        }
    }

    public SignInDto signIn(String phone, String password) {
        validatePhone(phone);

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid phone or password");
        }

        if (!user.isVerifiedPhone()) {
            throw new RuntimeException("Phone not verified");
        }

        if (!user.isActive()) {
            user.setActive(true);
            userRepository.save(user);
        }

        return buildSignInDto(user);
    }

    // ==================== Role Management ====================

    public UserDto addRole(UUID userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRoles().contains(newRole)) {
            throw new RuntimeException("User already has role: " + newRole);
        }

        if (newRole == UserRole.admin) {
            throw new RuntimeException("Cannot assign admin role through this endpoint");
        }

        user.getRoles().add(newRole);
        User updated = userRepository.save(user);
        return userMapper.toDto(updated);
    }

    // ==================== Password ====================

    public void requestChangePasswordOtp(UUID userId, String oldPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        userOtpService.generateAndSendOtp(user.getPhone());
    }

    public void confirmChangePassword(UUID userId, String otp, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isValid = userOtpService.validateOtp(user.getPhone(), otp);
        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setOtpPhone(null);
        userRepository.save(user);
    }

    // ==================== CRUD ====================

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toDto(user);
    }

    public UserDto updateUser(UUID id, UpdateUserForm form) {
        validatePhone(form.getPhone());

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.findByPhone(form.getPhone())
                .filter(existingUser -> !existingUser.getId().equals(id))
                .ifPresent(u -> { throw new RuntimeException("Phone already exists"); });

        user.setName(form.getName());
        user.setPhone(form.getPhone());

        if (form.getRoles() != null && !form.getRoles().isEmpty()) {
            user.setRoles(new HashSet<>(form.getRoles()));
        }

        return userMapper.toDto(userRepository.save(user));
    }

    public UserDto updateProfile(UUID userId, UpdateProfileForm form) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validatePhone(form.getPhone());

        userRepository.findByPhone(form.getPhone())
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> { throw new RuntimeException("Phone already exists"); });

        user.setName(form.getName());
        user.setPhone(form.getPhone());

        Path uploadDir = Paths.get(UPLOADED_FOLDER);
        if (form.getProfileImage() != null && !form.getProfileImage().isEmpty()) {
            if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

            byte[] bytes = ImageUtility.compressImage(form.getProfileImage().getBytes());
            String fileName = new Date().getTime() + "A-A" + form.getProfileImage().getOriginalFilename();
            Path path = uploadDir.resolve(fileName);
            Files.write(path, bytes);

            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            perms.add(PosixFilePermission.GROUP_READ);
            perms.add(PosixFilePermission.OTHERS_READ);
            Files.setPosixFilePermissions(path, perms);

            user.setProfileImage(fileName);
        }

        user.setActive(true);
        return userMapper.toDto(userRepository.save(user));
    }

    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id:" + id));
        user.setActive(false);
        userRepository.save(user);
    }

    public UserDto reactivateUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(true);
        return userMapper.toDto(userRepository.save(user));
    }

    public WalletDto getUserWallet(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return walletMapper.toDto(wallet);
    }

    public WalletDto updateUserBalance(UUID userId, double addedBalance) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) throw new RuntimeException("Cannot update balance for inactive user");

        return walletService.addBalance(userId, addedBalance);
    }

    // ==================== Private Helpers ====================

    /**
     * بناء SignInDto + token من الـ User
     */
    private SignInDto buildSignInDto(User user) {
        String rolesStr = user.getRoles().stream()
                .map(UserRole::name)
                .reduce((a, b) -> a + "," + b)
                .orElse(UserRole.user.name());

        String token = jwtTokenProvider.generateToken(user.getId(), user.getPhone(), rolesStr);

        SignInDto dto = userMapper.toSignInDto(user);
        dto.setToken(token);
        return dto;
    }

    private void validatePhone(String phone) {
        if (phone == null || !phone.startsWith("20") || phone.length() != 12) {
            throw new RuntimeException("Phone must start with 20 and be 12 digits");
        }
    }
}