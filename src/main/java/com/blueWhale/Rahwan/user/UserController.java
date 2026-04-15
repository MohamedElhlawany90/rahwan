package com.blueWhale.Rahwan.user;

import com.blueWhale.Rahwan.otp.UserOtpService;
import com.blueWhale.Rahwan.security.UserPrincipal;
import com.blueWhale.Rahwan.wallet.WalletDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** تسجيل يوزر جديد */
    @PostMapping("/signup")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(form));
    }

    /**
     * تسجيل من الـ driver app
     * - لو الرقم موجود: يتحقق من الباسورد ويضيف دور driver
     * - لو الرقم مش موجود: ينشئ أكونت جديد بدور driver
     */
    @PostMapping("/signup-driver")
    public ResponseEntity<SignInDto> signupAsDriver(@Valid @RequestBody UserForm form) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signupAsDriver(form));
    }

    /** تسجيل الدخول (user app و driver app) */
    @PostMapping("/signin")
    public ResponseEntity<SignInDto> signIn(@RequestBody SignInForm form) {
        return ResponseEntity.ok(userService.signIn(form.getPhone(), form.getPassword()));
    }

    /**
     * إضافة دور للمستخدم الحالي
     * القيم المسموح بيها: user, driver فقط — admin محمي في الـ Service
     * POST /api/users/add-role?role=driver
     */
    @PostMapping("/add-role")
    public ResponseEntity<UserDto> addRole(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam UserRole role
    ) {
        return ResponseEntity.ok(userService.addRole(principal.getId(), role));
    }

    /** Admin: تحديث بيانات مستخدم */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserForm form
    ) {
        return ResponseEntity.ok(userService.updateUser(id, form));
    }

    /** تحديث بروفايل المستخدم */
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

    /** طلب OTP لتغيير الباسورد */
    @PostMapping("/change-password/request-otp")
    public ResponseEntity<Void> requestChangePasswordOtp(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ChangePasswordRequest request
    ) {
        userService.requestChangePasswordOtp(principal.getId(), request.getOldPassword());
        return ResponseEntity.ok().build();
    }

    /** تأكيد تغيير الباسورد بـ OTP */
    @PostMapping("/change-password/confirm")
    public ResponseEntity<Void> confirmChangePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ConfirmChangePasswordRequest request
    ) {
        userService.confirmChangePassword(principal.getId(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/request-otp")
    public ResponseEntity<Void> forgotPasswordRequestOtp(
            @RequestBody ForgotPasswordRequest request
    ) {
        userService.forgotPasswordRequest(request.getPhone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password/confirm")
    public ResponseEntity<Void> forgotPasswordConfirm(
            @RequestBody ForgotPasswordConfirmRequest request
    ) {
        userService.forgotPasswordConfirm(
                request.getPhone(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok().build();
    }

    /** Admin: جلب كل المستخدمين */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /** جلب مستخدم بالـ ID */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** جلب محفظة المستخدم */
    @GetMapping("/{id}/wallet")
    public ResponseEntity<WalletDto> getUserWallet(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserWallet(id));
    }

    /** تعطيل الأكونت (logout) */
    @DeleteMapping("/{id}/logout")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    /** تفعيل الأكونت */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserDto> activateUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.reactivateUser(id));
    }

    /** إرسال OTP للتحقق من الرقم */
    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, String>> sendOtpSmsPhone(@RequestParam String phone) {
        userOtpService.generateAndSendOtp(phone);
        return ResponseEntity.ok(Map.of("message", "OTP sent to phone: " + phone));
    }

    /** التحقق من OTP */
    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, String>> verifyOtpPhone(
            @RequestParam String phone,
            @RequestParam String otp) {

        boolean verified = userOtpService.validateOtp(phone, otp);

        if (verified) {
            return ResponseEntity.ok(Map.of("message", "OTP Verified Successfully"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
    }

    /** Admin: إضافة رصيد للمستخدم */
    @PostMapping("/{userId}/add-balance")
    public ResponseEntity<WalletDto> addUserBalance(
            @PathVariable UUID userId,
            @RequestParam double amount) {

        if (amount <= 0) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(userService.updateUserBalance(userId, amount));
    }
}