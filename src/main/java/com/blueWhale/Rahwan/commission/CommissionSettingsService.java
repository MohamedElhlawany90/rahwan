package com.blueWhale.Rahwan.commission;

import com.blueWhale.Rahwan.exception.ResourceNotFoundException;
import com.blueWhale.Rahwan.order.Order;
import com.blueWhale.Rahwan.order.OrderRepository;
import com.blueWhale.Rahwan.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CommissionSettingsService {

    private final CommissionSettingsRepository settingsRepository;
    private final CommissionSettingsMapper settingsMapper;
    private final OrderRepository orderRepository;

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

    /**
     * حساب الإيرادات الشهرية من عمولات الطلبات
     */
    public List<MonthlyRevenueDto> getMonthlyRevenue(int year) {
        // جلب الطلبات المكتملة في السنة المحددة
        LocalDateTime startOfYear = LocalDateTime.of(year, 1, 1, 0, 0);
        LocalDateTime endOfYear = LocalDateTime.of(year, 12, 31, 23, 59, 59);

        List<Order> orders = orderRepository.findDeliveredOrdersBetweenDates(
                        OrderStatus.DELIVERED,
                        startOfYear,
                        endOfYear
        );

        // تجميع الإيرادات حسب الشهر
        Map<String, Double> monthlyRevenueMap = new LinkedHashMap<>();

        // تهيئة كل الشهور بـ 0
        String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        for (String month : months) {
            monthlyRevenueMap.put(month, 0.0);
        }

        // حساب الإيرادات لكل شهر
        for (Order order : orders) {
            String monthKey = order.getDeliveredAt().getMonth()
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase();

            double currentRevenue = monthlyRevenueMap.getOrDefault(monthKey, 0.0);
            monthlyRevenueMap.put(monthKey, currentRevenue + order.getAppCommission());
        }

        // تحويل إلى List of DTOs
        return monthlyRevenueMap.entrySet().stream()
                .map(entry -> MonthlyRevenueDto.builder()
                        .month(entry.getKey())
                        .revenue(Math.round(entry.getValue() * 100.0) / 100.0)
                        .build())
                .collect(Collectors.toList());
    }
}