package com.braidsbeautybyangie.paymentservice.model.request;

import com.braidsbeautybyangie.paymentservice.model.PaymentType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class RequestPayment {
    private String paymentProvider;
    private BigInteger paymentAccountNumber;
    private LocalTime paymentExpirationDate;
    private BigDecimal paymentTotalPrice;
    private boolean paymentIsDefault;
    private Long userId;
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;
}
