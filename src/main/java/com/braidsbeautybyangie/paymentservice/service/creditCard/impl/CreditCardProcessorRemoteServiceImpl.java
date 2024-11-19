package com.braidsbeautybyangie.paymentservice.service.creditCard.impl;

import com.braidsbeautybyangie.paymentservice.model.request.CreditCardProcessRequest;
import com.braidsbeautybyangie.paymentservice.rest.RestCreditCardProcessorAdapter;
import com.braidsbeautybyangie.paymentservice.service.creditCard.CreditCardProcessorRemoteService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.AppExceptions.CreditCardProcessorUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
@RequiredArgsConstructor
public class CreditCardProcessorRemoteServiceImpl implements CreditCardProcessorRemoteService {
    private final RestCreditCardProcessorAdapter restCreditCardProcessorAdapter;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CreditCardProcessorRemoteServiceImpl.class);
    @Override
    public void process(BigInteger cardNumber, BigDecimal paymentAmount) {
        try {
            var request = new CreditCardProcessRequest(cardNumber, paymentAmount);
            restCreditCardProcessorAdapter.processCreditCard(request);
        } catch (FeignException e) {
            LOGGER.error("Error processing credit card with the following number: {}", cardNumber);
            throw new CreditCardProcessorUnavailableException(e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error processing credit card with the following number: {}", cardNumber, e);
            throw new CreditCardProcessorUnavailableException(e);
        }
    }
}
