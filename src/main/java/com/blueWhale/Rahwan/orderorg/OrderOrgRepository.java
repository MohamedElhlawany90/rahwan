package com.blueWhale.Rahwan.orderorg;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderOrgRepository extends JpaRepository<OrderOrg, Long> {

    List<OrderOrg> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<OrderOrg> findByCharityIdOrderByCreatedAtDesc(Long charityId);

    List<OrderOrg> findByStatusOrderByCreatedAtDesc(OrderOrgStatus status);
}