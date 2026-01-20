package com.blueWhale.Rahwan.charity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CharityRepository extends JpaRepository<Charity, Long> {

    List<Charity> findByActiveTrue();

    Optional<Charity> findByPhone(String phone);
}