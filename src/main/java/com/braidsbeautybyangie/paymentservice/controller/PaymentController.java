package com.braidsbeautybyangie.paymentservice.controller;

import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.service.payment.PaymentService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.util.ApiResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@OpenAPIDefinition(
        info = @Info(
                title = "API-PAYMENT",
                version = "1.0",
                description = "Payment management"
        )
)
@RestController
@RequestMapping("/v1/payment-service/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @GetMapping(value="/{shopOrderId}")
    public ResponseEntity<ApiResponse> getPaymentByShopOrderId(@PathVariable(name = "shopOrderId") Long shopOrderId){
        return ResponseEntity.ok(ApiResponse.ok(" find shop order id", paymentService.findPaymentByShopOrderId(shopOrderId)));
    }
}
