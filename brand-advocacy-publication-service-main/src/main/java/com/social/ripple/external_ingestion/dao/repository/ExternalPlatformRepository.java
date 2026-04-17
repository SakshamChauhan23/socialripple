package com.social.ripple.external_ingestion.dao.repository;

import com.social.ripple.external_ingestion.dao.model.ExternalPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExternalPlatformRepository extends JpaRepository<ExternalPlatform, Long> {

    List<ExternalPlatform> findByOrganizationIdOrderByPlatformNameAsc(Long organizationId);

    Optional<ExternalPlatform> findByOrganizationIdAndPlatformNameIgnoreCase(Long organizationId, String platformName);

    @Query("SELECT ep FROM ExternalPlatform ep WHERE UPPER(ep.platformName) IN :platformNames AND ep.enabled = true")
    List<ExternalPlatform> findEnabledByPlatformNames(@Param("platformNames") Collection<String> platformNames);
}
