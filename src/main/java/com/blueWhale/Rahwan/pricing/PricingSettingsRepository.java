package com.blueWhale.Rahwan.pricing;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PricingSettingsRepository extends JpaRepository<PricingSettings, Long> {

    Optional<PricingSettings> findBySettingKeyAndActiveTrue(String settingKey);

    Optional<PricingSettings> findFirstByActiveTrueOrderByCreatedAtDesc();
}