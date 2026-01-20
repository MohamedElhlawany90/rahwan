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
        try {
            UserDto dto = userService.createUser(form);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<UserDto> signIn(@RequestBody SignInForm form) {
        try {
            return ResponseEntity.ok(userService.signIn(form.getPhone(), form.getPassword()));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserForm form) {
        try {
            return ResponseEntity.ok(userService.updateUser(id, form));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping(value = "/profile/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> updateProfile(
            @PathVariable UUID id,
            @RequestPart("name") String name,
            @RequestPart("phone") String phone,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            UpdateProfileForm form = UpdateProfileForm.builder()
                    .name(name)
                    .phone(phone)
                    .profileImage(image)
                    .build();

            return ResponseEntity.ok(userService.updateProfile(id, form));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(userService.getUserById(id));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/{id}/wallet")
    public ResponseEntity<WalletDto> getUserWallet(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(userService.getUserWallet(id));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @DeleteMapping("/{id}/logout")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable UUID id) {
        try {
            UserDto dto = userService.reactivateUser(id);
            return ResponseEntity.ok(dto);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtpSmsPhone(@RequestParam String phone) {
        try {
            String otp = userOtpService.generateAndSendOtp(phone);
            return ResponseEntity.ok(Map.of("message", "OTP sent to phone: " + phone));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtpPhone(
            @RequestParam String phone,
            @RequestParam String otp) {
        try {
            boolean verified = userOtpService.validateOtp(phone, otp);

            if (verified) {
                return ResponseEntity.ok(Map.of("message", "OTP Verified Successfully"));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid OTP"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    /**
     * إضافة رصيد للمستخدم - يرجع WalletDto كامل مع التفاصيل
     */
    @PostMapping("/{userId}/add-balance")
    public ResponseEntity<WalletDto> addUserBalance(
            @PathVariable UUID userId,
            @RequestParam double amount) {
        try {
            if (amount <= 0) {
                return ResponseEntity.badRequest().body(null);
            }
            WalletDto walletDto = userService.updateUserBalance(userId, amount);
            return ResponseEntity.ok(walletDto);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}