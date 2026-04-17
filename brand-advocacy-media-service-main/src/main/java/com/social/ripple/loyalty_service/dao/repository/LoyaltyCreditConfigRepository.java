package com.social.ripple.loyalty_service.dao.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.social.ripple.loyalty_service.dao.model.LoyaltyCreditConfig;

@Repository
public interface LoyaltyCreditConfigRepository extends JpaRepository<LoyaltyCreditConfig, Long> {

	Optional<LoyaltyCreditConfig> findByCreditId(long creditId);
}
