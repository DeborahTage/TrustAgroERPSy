package com.trustagro.procurement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "proc_number_sequences")
@Data
public class ProcNumberSequence {

    @Id
    @Column(name = "name", length = 40, nullable = false)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;
}
