package com.trustagro.hr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Gap-free document counters. Rows are taken under a pessimistic write lock so
 * two concurrent requests cannot mint the same employee or payslip number.
 */
@Entity
@Table(name = "hr_number_sequences")
@Data
public class HrNumberSequence {

    @Id
    @Column(name = "name", length = 40, nullable = false)
    private String name;

    @Column(name = "next_value", nullable = false)
    private long nextValue = 1L;
}
