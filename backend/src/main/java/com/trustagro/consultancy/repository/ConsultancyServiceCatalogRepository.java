package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyServiceCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyServiceCatalogRepository extends JpaRepository<ConsultancyServiceCatalog, Long> {
    Optional<ConsultancyServiceCatalog> findByServiceCode(String serviceCode);
    boolean existsByServiceCode(String serviceCode);
    List<ConsultancyServiceCatalog> findByActiveTrueOrderByNameAsc();
}
