package com.braidsbeautybyangie.paymentservice.service.handler;

import com.braidsbeautybyangie.paymentservice.model.PaymentType;
import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.service.payment.PaymentService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.AppExceptions.CreditCardProcessorUnavailableException;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.commands.ProcessPaymentCommand;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.dto.ServiceCore;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.events.PaymentFailedEvent;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.events.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@KafkaListener(topics = "${payments.commands.topic.name}")
@RequiredArgsConstructor
public class PaymentsCommandsHandler {

    private final PaymentService paymentService;
    private static final Logger logger = LoggerFactory.getLogger(PaymentsCommandsHandler.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${payments.events.topic.name}")
    private String paymentEventsTopicName;

    @KafkaHandler
    public void handleCommand(@Payload ProcessPaymentCommand command) {
        boolean isService = false;
        boolean isProduct = false;
        BigDecimal totalPrice = BigDecimal.ZERO;
        try {
            if (command.getProductList().isEmpty()) {
                // Calcular el total de servicios
                isService = true;
                BigDecimal totalPriceServices = command.getServiceList()
                        .stream()
                        .map(ServiceCore::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalPrice = totalPrice.add(totalPriceServices);

            } else {
                // Calcular el total de productos
                isProduct= true;
                BigDecimal totalPriceProducts = command.getProductList()
                        .stream()
                        .map(product -> BigDecimal.valueOf(product.getPrice())
                                .multiply(BigDecimal.valueOf(product.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                totalPrice = totalPrice.add(totalPriceProducts);
            }
            PaymentDTO paymentDTO = PaymentDTO.builder()
                    .paymentType(PaymentType.CREDIT_CARD)
                    .paymentProvider(command.getPaymentProvider())
                    .userId(command.getUserId())
                    .paymentAccountNumber(command.getPaymentAccountNumber())
                    .paymentIsDefault(command.isPaymentIsDefault())
                    .paymentExpirationDate(command.getPaymentExpirationDate())
                    .paymentTotalPrice(totalPrice)
                    .shopOrderId(command.getShopOrderId())
                    .build();
            PaymentDTO paymentDTOSaved = paymentService.processPayment(paymentDTO);
            PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
                    .paymentId(paymentDTOSaved.getPaymentId())
                    .shopOrderId(command.getShopOrderId())
                    .isProduct(isProduct)
                    .isService(isService)
                    .build();
            kafkaTemplate.send(paymentEventsTopicName, paymentProcessedEvent);

        } catch (CreditCardProcessorUnavailableException e) {
            logger.error("Error in PaymentsCommandsHandler.handleCommand: {}", e.getMessage());
            PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                    .shopOrderId(command.getShopOrderId())
                    .productList(command.getProductList())
                    .reservationId(command.getReservationId())
                    .build();
            kafkaTemplate.send(paymentEventsTopicName, paymentFailedEvent);
        }
    }
}
