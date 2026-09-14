package com.trustagro.training.dto;

import com.trustagro.training.entity.RegistrationStatus;
import com.trustagro.training.entity.TrainingPaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TrainingRegistrationResponse {
    private Long id;
    private String registrationNumber;
    private Long sessionId;
    private String sessionCode;
    private Long programId;
    private String programTitle;
    private Long clientId;
    private String participantName;
    private String participantPhone;
    private String participantEmail;
    private String organization;
    private String jobTitle;
    private LocalDate registrationDate;
    private RegistrationStatus status;
    private BigDecimal price;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal outstandingAmount;
    private TrainingPaymentStatus paymentStatus;
    private Long invoiceId;
    private String invoiceNumber;
    private boolean completed;
    private LocalDateTime completedAt;
    private String notes;
    private LocalDateTime createdAt;
    private List<AttendanceResponse> attendance;
    private CertificateResponse certificate;
    private List<PaymentResponse> payments;

    @Data
    @Builder
    public static class AttendanceResponse {
        private Long id;
        private LocalDate sessionDate;
        private String status;
        private LocalDateTime checkInTime;
    }

    @Data
    @Builder
    public static class CertificateResponse {
        private Long id;
        private String certificateNumber;
        private String verificationCode;
        private String status;
        private LocalDate issueDate;
        private String certificateType;
    }

    @Data
    @Builder
    public static class PaymentResponse {
        private Long id;
        private BigDecimal amount;
        private String paymentMethod;
        private LocalDate paymentDate;
        private Long financeTransactionId;
    }
}
