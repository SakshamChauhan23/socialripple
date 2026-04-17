package com.social.ripple.loyalty_service.dao.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.social.ripple.loyalty_service.dao.model.LoyaltyTransaction;

@Repository
public interface LoyaltyTransactionRepository extends JpaRepository<LoyaltyTransaction, Long> {
    
    List<LoyaltyTransaction> findByUserId(Long userId);
}
