package com.braidsbeautybyangie.paymentservice.rest;

import com.braidsbeautybyangie.paymentservice.model.request.CreditCardProcessRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "credit-card-processor-service")
public interface RestCreditCardProcessorAdapter {

    @PostMapping("/v1/payment-service/ccp/process")
    void processCreditCard(@RequestBody CreditCardProcessRequest request);
}
