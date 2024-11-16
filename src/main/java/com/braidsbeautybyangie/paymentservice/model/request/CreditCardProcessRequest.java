package com.braidsbeautybyangie.paymentservice.model.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardProcessRequest {
    private BigInteger creditCardNumber;
    private BigDecimal paymentAmount;
}
