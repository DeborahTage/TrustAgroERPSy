package com.trustagro.crm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "crm_number_sequences")
@Data
public class CrmNumberSequence {

    @Id
    @Column(name = "name", length = 40, nullable = false)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;
}
