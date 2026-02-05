package com.blueWhale.Rahwan.pricing;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/settings/pricing")
@RequiredArgsConstructor
public class PricingSettingsController {

    private final PricingSettingsService settingsService;

    /**
     * جلب الإعدادات النشطة (متاح للجميع)
     */
    @GetMapping("/active")
    public ResponseEntity<PricingSettingsDto> getActiveSettings() {
        PricingSettingsDto settings = settingsService.getActiveSettingsDto();
        return ResponseEntity.ok(settings);
    }

    /**
     * تحديث الإعدادات (Admin فقط)
     */
    @PutMapping
    public ResponseEntity<PricingSettingsDto> updateSettings(@Valid @RequestBody PricingSettingsForm form) {
        PricingSettingsDto updated = settingsService.updateSettings(form);
        return ResponseEntity.ok(updated);
    }

    /**
     * جلب كل الإعدادات (Admin فقط)
     */
    @GetMapping
    public ResponseEntity<List<PricingSettingsDto>> getAllSettings() {
        List<PricingSettingsDto> settings = settingsService.getAllSettings();
        return ResponseEntity.ok(settings);
    }

    /**
     * جلب إعدادات محددة (Admin فقط)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PricingSettingsDto> getSettingsById(@PathVariable Long id) {
        PricingSettingsDto settings = settingsService.getSettingsById(id);
        return ResponseEntity.ok(settings);
    }

    /**
     * تفعيل إعدادات محددة (Admin فقط)
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<PricingSettingsDto> activateSettings(@PathVariable Long id) {
        PricingSettingsDto activated = settingsService.activateSettings(id);
        return ResponseEntity.ok(activated);
    }
}