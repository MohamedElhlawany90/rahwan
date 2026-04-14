package com.blueWhale.Rahwan.commission;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/settings/commission")
@RequiredArgsConstructor
public class CommissionSettingsController {

    private final CommissionSettingsService settingsService;

    /**
     * جلب الإعدادات النشطة (متاح للجميع)
     */
    @GetMapping("/active")
    public ResponseEntity<CommissionSettingsDto> getActiveSettings() {
        CommissionSettingsDto settings = settingsService.getActiveSettingsDto();
        return ResponseEntity.ok(settings);
    }

    /**
     * تحديث الإعدادات (Admin فقط)
     */
    @PutMapping
    public ResponseEntity<CommissionSettingsDto> updateSettings(@Valid @RequestBody CommissionSettingsForm form) {
        CommissionSettingsDto updated = settingsService.updateSettings(form);
        return ResponseEntity.ok(updated);
    }

    /**
     * جلب كل الإعدادات (Admin فقط)
     */
    @GetMapping
    public ResponseEntity<List<CommissionSettingsDto>> getAllSettings() {
        List<CommissionSettingsDto> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * جلب إعدادات محددة (Admin فقط)
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommissionSettingsDto> getSettingsById(@PathVariable Long id) {
        CommissionSettingsDto settings = settingsService.getSettingsById(id);
        return ResponseEntity.ok(settings);
    }

    /**
     * تفعيل إعدادات محددة (Admin فقط)
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<CommissionSettingsDto> activateSettings(@PathVariable Long id) {
        CommissionSettingsDto activated = settingsService.activateSettings(id);
        return ResponseEntity.ok(activated);
    }

    /**
     * جلب الإيرادات الشهرية (Admin فقط)
     */
    @GetMapping("/monthly-revenue")
    public ResponseEntity<List<MonthlyRevenueDto>> getMonthlyRevenue(
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : Year.now().getValue();
        List<MonthlyRevenueDto> revenue = settingsService.getMonthlyRevenue(targetYear);
        return ResponseEntity.ok(revenue);
    }
}