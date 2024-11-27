package com.braidsbeautybyangie.paymentservice.service.handler;

import com.braidsbeautybyangie.paymentservice.model.PaymentType;
import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.service.payment.PaymentService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.AppExceptions.CreditCardProcessorUnavailableException;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.commands.ProcessPaymentCommand;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.events.PaymentFailedEvent;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.events.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PaymentsCommandsHandler {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${payments.events.topic.name}")
    private String paymentEventsTopicName;

    @KafkaHandler
    public void handleCommand(@Payload ProcessPaymentCommand command) {
        try {
            BigDecimal totalPrice = calculateTotalPrice(command);

            PaymentDTO paymentDTO = buildPaymentDTO(command, totalPrice);

            PaymentDTO paymentDTOSaved = paymentService.processPayment(paymentDTO);

            publishPaymentProcessedEvent(command, paymentDTOSaved, totalPrice);

        } catch (CreditCardProcessorUnavailableException e) {
            log.error("Error in PaymentsCommandsHandler.handleCommand: {}", e.getMessage());
            publishPaymentFailedEvent(command);
        }
    }

    private BigDecimal calculateTotalPrice(ProcessPaymentCommand command) {
        BigDecimal totalPriceProducts = calculateProductsTotal(command);
        BigDecimal totalPriceServices = command.getReservationCore().getTotalPrice();
        return totalPriceProducts.add(totalPriceServices);
    }

    private BigDecimal calculateProductsTotal(ProcessPaymentCommand command) {
        if (command.getProductList().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return command.getProductList().stream()
                .map(product -> product.getPrice()
                        .multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentDTO buildPaymentDTO(ProcessPaymentCommand command, BigDecimal totalPrice) {
        return PaymentDTO.builder()
                .paymentType(PaymentType.CREDIT_CARD)
                .paymentProvider(command.getPaymentProvider())
                .userId(command.getUserId())
                .paymentAccountNumber(command.getPaymentAccountNumber())
                .paymentIsDefault(command.isPaymentIsDefault())
                .paymentExpirationDate(command.getPaymentExpirationDate())
                .paymentTotalPrice(totalPrice)
                .shopOrderId(command.getShopOrderId())
                .build();
    }

    private void publishPaymentProcessedEvent(ProcessPaymentCommand command, PaymentDTO paymentDTOSaved, BigDecimal totalPrice) {
        boolean isService = command.getReservationCore().getReservationId() != null;

        PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
                .paymentId(paymentDTOSaved.getPaymentId())
                .shopOrderId(command.getShopOrderId())
                .paymentTotalPrice(totalPrice)
                .isProduct(!command.getProductList().isEmpty())
                .isService(isService)
                .build();
        log.info("PaymentProcessedEvent: {}", paymentProcessedEvent);
        try {
            kafkaTemplate.send(paymentEventsTopicName, paymentProcessedEvent);
            log.info("PaymentProcessedEvent published: {}", paymentProcessedEvent);
        } catch (Exception e) {
            log.error("Error in PaymentsCommandsHandler.publishPaymentProcessedEvent: {}", e.getMessage());
        }

    }

    private void publishPaymentFailedEvent(ProcessPaymentCommand command) {
        PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                .shopOrderId(command.getShopOrderId())
                .productList(command.getProductList())
                .reservationId(command.getReservationCore().getReservationId())
                .build();
        try {
            log.info("PaymentFailedEvent: {}", paymentFailedEvent);
            kafkaTemplate.send(paymentEventsTopicName, paymentFailedEvent);
            log.info("PaymentFailedEvent published: {}", paymentFailedEvent);
        } catch (Exception e) {
            log.error("Error in PaymentsCommandsHandler.publishPaymentFailedEvent: {}", e.getMessage());
        }
    }

}
