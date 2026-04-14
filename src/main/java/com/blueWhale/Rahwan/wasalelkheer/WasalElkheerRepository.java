package com.blueWhale.Rahwan.wasalelkheer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WasalElkheerRepository extends JpaRepository<WasalElkheer, Long> {

    List<WasalElkheer> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<WasalElkheer> findByCharityIdOrderByCreatedAtDesc(Long charityId);

    List<WasalElkheer> findByStatusOrderByCreatedAtDesc(WasalElkheerStatus status);
}