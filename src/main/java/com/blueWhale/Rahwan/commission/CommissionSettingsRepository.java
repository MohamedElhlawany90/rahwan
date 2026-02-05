package com.blueWhale.Rahwan.commission;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommissionSettingsRepository extends JpaRepository<CommissionSettings, Long> {

    Optional<CommissionSettings> findBySettingKeyAndActiveTrue(String settingKey);

    Optional<CommissionSettings> findFirstByActiveTrueOrderByCreatedAtDesc();
}