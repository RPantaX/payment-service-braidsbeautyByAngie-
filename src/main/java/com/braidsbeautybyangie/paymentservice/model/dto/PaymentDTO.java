package com.braidsbeautybyangie.paymentservice.model.dto;

import com.braidsbeautybyangie.paymentservice.model.PaymentType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PaymentDTO {
    private Long paymentId;
    private String paymentProvider;
    private BigInteger paymentAccountNumber;
    private LocalTime paymentExpirationDate;
    private boolean paymentIsDefault;
    private BigDecimal paymentTotalPrice;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
}
