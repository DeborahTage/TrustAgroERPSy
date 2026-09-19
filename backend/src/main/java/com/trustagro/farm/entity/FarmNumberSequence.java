package com.trustagro.farm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "farm_number_sequences")
@Data
public class FarmNumberSequence {

    @Id
    @Column(name = "name", length = 40, nullable = false)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;
}
