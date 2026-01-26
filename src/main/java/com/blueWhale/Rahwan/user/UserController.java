package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.otp.UserOtpService;
import com.blueWhale.Rahwan.wallet.WalletDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserOtpService userOtpService;

    public UserController(UserService userService, UserOtpService userOtpService) {
        this.userService = userService;
        this.userOtpService = userOtpService;
    }

    @PostMapping("/signup")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserForm form) {

        UserDto dto = userService.createUser(form);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);

    }

    @PostMapping("/signin")
    public ResponseEntity<UserDto> signIn(@RequestBody SignInForm form) {

        return ResponseEntity.ok(userService.signIn(form.getPhone(), form.getPassword()));

    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserForm form) {
        return ResponseEntity.ok(userService.updateUser(id, form));

    }

    @PostMapping(value = "/profile/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable UUID id,
            @RequestPart("name") String name,
            @RequestPart("phone") String phone,
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        UpdateProfileForm form = UpdateProfileForm.builder()
                .name(name)
                .phone(phone)
                .profileImage(image)
                .build();

        return ResponseEntity.ok(userService.updateProfile(id, form));

    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {

        return ResponseEntity.ok(userService.getUserById(id));

    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<WalletDto> getUserWallet(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserWallet(id));

    }

    @DeleteMapping("/{id}/logout")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {

        userService.deleteUser(id);
        return ResponseEntity.noContent().build();

    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable UUID id) {

        UserDto dto = userService.reactivateUser(id);
        return ResponseEntity.ok(dto);

    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtpSmsPhone(@RequestParam String phone) {
        String otp = userOtpService.generateAndSendOtp(phone);
        return ResponseEntity.ok(Map.of("message", "OTP sent to phone: " + phone));

    }


    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtpPhone(
            @RequestParam String phone,
            @RequestParam String otp) {

        boolean verified = userOtpService.validateOtp(phone, otp);

        if (verified) {
            return ResponseEntity.ok(Map.of("message", "OTP Verified Successfully"));
        }
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid OTP"));

    }

    /**
     * إضافة رصيد للمستخدم - يرجع WalletDto كامل مع التفاصيل
     */
    @PostMapping("/{userId}/add-balance")
    public ResponseEntity<WalletDto> addUserBalance(
            @PathVariable UUID userId,
            @RequestParam double amount) {

        if (amount <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        WalletDto walletDto = userService.updateUserBalance(userId, amount);
        return ResponseEntity.ok(walletDto);

    }
}