package com.trustagro.crm.repository;

import com.trustagro.crm.entity.ClientStatus;
import com.trustagro.crm.entity.CrmClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CrmClientRepository extends JpaRepository<CrmClient, Long> {
    List<CrmClient> findByStatus(ClientStatus status);
    Optional<CrmClient> findByCustomerCode(String customerCode);

    @Query("SELECT c FROM CrmClient c WHERE " +
           "(:email IS NOT NULL AND LOWER(c.email) = LOWER(:email)) OR " +
           "(:phone IS NOT NULL AND c.phone = :phone)")
    List<CrmClient> findByEmailOrPhone(@Param("email") String email, @Param("phone") String phone);
}
