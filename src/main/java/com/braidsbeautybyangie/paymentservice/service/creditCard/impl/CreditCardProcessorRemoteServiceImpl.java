package com.braidsbeautybyangie.paymentservice.service.creditCard.impl;

import com.braidsbeautybyangie.paymentservice.model.request.CreditCardProcessRequest;
import com.braidsbeautybyangie.paymentservice.rest.RestCreditCardProcessorAdapter;
import com.braidsbeautybyangie.paymentservice.service.creditCard.CreditCardProcessorRemoteService;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;

@Service
public class CreditCardProcessorRemoteServiceImpl implements CreditCardProcessorRemoteService {
    private RestCreditCardProcessorAdapter restCreditCardProcessorAdapter;
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(CreditCardProcessorRemoteServiceImpl.class);
    @Override
    public void process(BigInteger cardNumber, BigDecimal paymentAmount) {
        try {
            var request = new CreditCardProcessRequest(cardNumber, paymentAmount);
            restCreditCardProcessorAdapter.processCreditCard(request);
        } catch (Exception e) {
            LOGGER.error("Error processing credit card with the following number: {}", cardNumber);
            //todo: handle exception with kafka
            throw new RuntimeException(e);
        }
    }
}
