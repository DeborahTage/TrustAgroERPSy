package com.trustagro.farm.document.repository;

import com.trustagro.farm.document.entity.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, Long> {
    Optional<DocumentTemplate> findByCode(String code);
}
