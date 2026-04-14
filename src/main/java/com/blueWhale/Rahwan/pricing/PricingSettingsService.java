package com.blueWhale.Rahwan.pricing;


import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PricingSettingsService {

    private final PricingSettingsRepository settingsRepository;
    private final PricingSettingsMapper settingsMapper;

    /**
     * جلب الإعدادات النشطة
     */
    public PricingSettings getActiveSettings() {
        return settingsRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * جلب الإعدادات النشطة كـ DTO
     */
    public PricingSettingsDto getActiveSettingsDto() {
        PricingSettings settings = getActiveSettings();
        return settingsMapper.toDto(settings);
    }

    /**
     * إنشاء إعدادات افتراضية
     */
    private PricingSettings createDefaultSettings() {
        PricingSettings defaultSettings = PricingSettings.builder()
                .settingKey("DEFAULT_PRICING")
                .baseCost(20.0)
                .costPerKm(2.5)
                .roadMultiplier(1.2)
                .active(true)
                .build();

        return settingsRepository.save(defaultSettings);
    }

    /**
     * تحديث الإعدادات
     */
    public PricingSettingsDto updateSettings(PricingSettingsForm form) {
        PricingSettings activeSettings = getActiveSettings();

        activeSettings.setBaseCost(form.getBaseCost());
        activeSettings.setCostPerKm(form.getCostPerKm());
        activeSettings.setRoadMultiplier(form.getRoadMultiplier());

        PricingSettings updated = settingsRepository.save(activeSettings);
        return settingsMapper.toDto(updated);
    }

    /**
     * جلب كل الإعدادات
     */
    public List<PricingSettingsDto> getAllSettings() {
        return settingsRepository.findAll()
                .stream()
                .map(settingsMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * جلب إعدادات محددة
     */
    public PricingSettingsDto getSettingsById(Long id) {
        PricingSettings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with id: " + id));
        return settingsMapper.toDto(settings);
    }

    /**
     * تفعيل إعدادات محددة
     */
    public PricingSettingsDto activateSettings(Long id) {
        // إلغاء تفعيل الحالية
        settingsRepository.findAll().forEach(s -> {
            s.setActive(false);
            settingsRepository.save(s);
        });

        // تفعيل الجديدة
        PricingSettings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with id: " + id));

        settings.setActive(true);
        PricingSettings updated = settingsRepository.save(settings);

        return settingsMapper.toDto(updated);
    }
}