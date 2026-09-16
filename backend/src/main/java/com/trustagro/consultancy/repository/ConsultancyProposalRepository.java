package com.trustagro.consultancy.repository;

import com.trustagro.consultancy.entity.ConsultancyProposal;
import com.trustagro.consultancy.entity.ProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyProposalRepository extends JpaRepository<ConsultancyProposal, Long> {
    Optional<ConsultancyProposal> findByProposalNumber(String proposalNumber);
    List<ConsultancyProposal> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<ConsultancyProposal> findByRequestIdOrderByCreatedAtDesc(Long requestId);
    List<ConsultancyProposal> findByStatusOrderByCreatedAtDesc(ProposalStatus status);
}
