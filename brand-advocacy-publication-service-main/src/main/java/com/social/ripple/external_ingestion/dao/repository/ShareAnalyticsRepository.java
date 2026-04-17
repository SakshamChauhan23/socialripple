package com.social.ripple.external_ingestion.dao.repository;

import com.social.ripple.external_ingestion.dao.model.ShareAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ShareAnalyticsRepository extends JpaRepository<ShareAnalytics, Long> {

    Optional<ShareAnalytics> findByExternalShareId(Long externalShareId);

    @Query(value = "SELECT YEAR(es.shared_at) as yr, MONTH(es.shared_at) as mo, SUM(sa.reach) as total_reach " +
            "FROM share_analytics sa " +
            "JOIN external_shares es ON sa.external_share_id = es.id " +
            "WHERE sa.tenant_id = :tenantId " +
            "GROUP BY YEAR(es.shared_at), MONTH(es.shared_at) " +
            "ORDER BY yr, mo", nativeQuery = true)
    List<Object[]> findMonthlyReachByTenant(@Param("tenantId") Long tenantId);

    @Query(value = "SELECT sa.platform, SUM(sa.impressions) as total_impressions, SUM(sa.clicks) as total_clicks " +
            "FROM share_analytics sa " +
            "WHERE sa.tenant_id = :tenantId " +
            "GROUP BY sa.platform", nativeQuery = true)
    List<Object[]> findLeadConversionByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT sa.externalShareId FROM ShareAnalytics sa")
    Set<Long> findAllExternalShareIds();
}
