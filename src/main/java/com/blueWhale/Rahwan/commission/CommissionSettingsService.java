package com.blueWhale.Rahwan.commission;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CommissionSettingsService {

    private final CommissionSettingsRepository settingsRepository;
    private final CommissionSettingsMapper settingsMapper;

    /**
     * جلب الإعدادات النشطة
     */
    public CommissionSettings getActiveSettings() {
        return settingsRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElseGet(this::createDefaultSettings);
    }

    /**
     * جلب الإعدادات النشطة كـ DTO
     */
    public CommissionSettingsDto getActiveSettingsDto() {
        CommissionSettings settings = getActiveSettings();
        return settingsMapper.toDto(settings);
    }

    /**
     * إنشاء إعدادات افتراضية
     */
    private CommissionSettings createDefaultSettings() {
        CommissionSettings defaultSettings = CommissionSettings.builder()
                .settingKey("DEFAULT_COMMISSION")
                .commissionRate(10.0) // 10% عمولة افتراضية
                .active(true)
                .build();

        return settingsRepository.save(defaultSettings);
    }

    /**
     * تحديث الإعدادات
     */
    public CommissionSettingsDto updateSettings(CommissionSettingsForm form) {
        CommissionSettings activeSettings = getActiveSettings();

        activeSettings.setCommissionRate(form.getCommissionRate());

        CommissionSettings updated = settingsRepository.save(activeSettings);
        return settingsMapper.toDto(updated);
    }

    /**
     * جلب كل الإعدادات
     */
    public List<CommissionSettingsDto> getAllSettings() {
        return settingsRepository.findAll()
                .stream()
                .map(settingsMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * جلب إعدادات محددة
     */
    public CommissionSettingsDto getSettingsById(Long id) {
        CommissionSettings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with id: " + id));
        return settingsMapper.toDto(settings);
    }

    /**
     * تفعيل إعدادات محددة
     */
    public CommissionSettingsDto activateSettings(Long id) {
        // إلغاء تفعيل الحالية
        settingsRepository.findAll().forEach(s -> {
            s.setActive(false);
            settingsRepository.save(s);
        });

        // تفعيل الجديدة
        CommissionSettings settings = settingsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Settings not found with id: " + id));

        settings.setActive(true);
        CommissionSettings updated = settingsRepository.save(settings);

        return settingsMapper.toDto(updated);
    }
}