package com.braidsbeautybyangie.paymentservice.service.creditCard.impl;

import com.braidsbeautybyangie.paymentservice.model.request.CreditCardProcessRequest;
import com.braidsbeautybyangie.paymentservice.rest.RestCreditCardProcessorAdapter;
import com.braidsbeautybyangie.paymentservice.service.creditCard.CreditCardProcessorRemoteService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.AppExceptions.CreditCardProcessorUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditCardProcessorRemoteServiceImpl implements CreditCardProcessorRemoteService {
    private final RestCreditCardProcessorAdapter restCreditCardProcessorAdapter;
    @Override
    public void process(BigInteger cardNumber, BigDecimal paymentAmount) {
        try {
            log.info("Processing credit card with the following number: {}", cardNumber);
            var request = new CreditCardProcessRequest(cardNumber, paymentAmount);
            restCreditCardProcessorAdapter.processCreditCard(request);
        } catch (FeignException e) {
            log.error("Error processing credit card with the following number: {}", cardNumber);
            throw new CreditCardProcessorUnavailableException(e);
        } catch (Exception e) {
            log.error("Unexpected error processing credit card with the following number: {}", cardNumber, e);
            throw new CreditCardProcessorUnavailableException(e);
        }
    }
}
