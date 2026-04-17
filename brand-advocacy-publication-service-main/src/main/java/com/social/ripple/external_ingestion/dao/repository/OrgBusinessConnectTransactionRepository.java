package com.social.ripple.external_ingestion.dao.repository;

import com.social.ripple.external_ingestion.dao.model.OrgBusinessConnectTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrgBusinessConnectTransactionRepository extends JpaRepository<OrgBusinessConnectTransaction, Long> {
    Optional<OrgBusinessConnectTransaction> findByTransactionId(String transactionId);

    Optional<OrgBusinessConnectTransaction> findByTemporaryAccessToken(String temporaryAccessToken);
}
