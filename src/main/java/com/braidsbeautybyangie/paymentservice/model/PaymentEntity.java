package com.braidsbeautybyangie.paymentservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
@Entity
@Table(name = "payment")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;
    @Column(name = "payment_provider", nullable = false)
    private String paymentProvider;
    @Column(name = "payment_account_number", nullable = false)
    private BigInteger paymentAccountNumber;
    @Column(name = "payment_expiration_date", nullable = false)
    private LocalTime paymentExpirationDate;
    @Column(name = "payment_total_price", nullable = false)
    private BigDecimal paymentTotalPrice;
    @Column(name = "paymen_is_default", nullable = false)
    private boolean paymentIsDefault;
    @Column(name = "user_id", nullable = true)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_entity", nullable = false)
    private PaymentType paymentType;
}
