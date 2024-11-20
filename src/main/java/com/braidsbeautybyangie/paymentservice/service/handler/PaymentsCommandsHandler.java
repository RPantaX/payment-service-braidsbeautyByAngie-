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
        try {
            BigDecimal totalPrice = calculateTotalPrice(command);

            PaymentDTO paymentDTO = buildPaymentDTO(command, totalPrice);

            PaymentDTO paymentDTOSaved = paymentService.processPayment(paymentDTO);

            publishPaymentProcessedEvent(command, paymentDTOSaved);

        } catch (CreditCardProcessorUnavailableException e) {
            logger.error("Error in PaymentsCommandsHandler.handleCommand: {}", e.getMessage());
            publishPaymentFailedEvent(command);
        }
    }

    private BigDecimal calculateTotalPrice(ProcessPaymentCommand command) {
        BigDecimal totalPriceProducts = calculateProductsTotal(command);
        BigDecimal totalPriceServices = calculateServicesTotal(command);
        return totalPriceProducts.add(totalPriceServices);
    }

    private BigDecimal calculateProductsTotal(ProcessPaymentCommand command) {
        if (command.getProductList().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return command.getProductList().stream()
                .map(product -> BigDecimal.valueOf(product.getPrice())
                        .multiply(BigDecimal.valueOf(product.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateServicesTotal(ProcessPaymentCommand command) {
        if (command.getServiceList().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return command.getServiceList().stream()
                .map(ServiceCore::getPrice)
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

    private void publishPaymentProcessedEvent(ProcessPaymentCommand command, PaymentDTO paymentDTOSaved) {
        PaymentProcessedEvent paymentProcessedEvent = PaymentProcessedEvent.builder()
                .paymentId(paymentDTOSaved.getPaymentId())
                .shopOrderId(command.getShopOrderId())
                .isProduct(!command.getProductList().isEmpty())
                .isService(!command.getServiceList().isEmpty())
                .build();

        kafkaTemplate.send(paymentEventsTopicName, paymentProcessedEvent);
    }

    private void publishPaymentFailedEvent(ProcessPaymentCommand command) {
        PaymentFailedEvent paymentFailedEvent = PaymentFailedEvent.builder()
                .shopOrderId(command.getShopOrderId())
                .productList(command.getProductList())
                .reservationId(command.getReservationId())
                .build();

        kafkaTemplate.send(paymentEventsTopicName, paymentFailedEvent);
    }

}
