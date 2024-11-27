package com.braidsbeautybyangie.paymentservice.service.payment.impl;


import com.braidsbeautybyangie.paymentservice.model.PaymentEntity;
import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.model.mapper.PaymentMapper;
import com.braidsbeautybyangie.paymentservice.model.response.ResponseListPageablePayments;
import com.braidsbeautybyangie.paymentservice.repository.PaymentRepository;
import com.braidsbeautybyangie.paymentservice.service.creditCard.CreditCardProcessorRemoteService;
import com.braidsbeautybyangie.paymentservice.service.payment.PaymentService;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.AppExceptions.AppExceptionNotFound;
import com.braidsbeautybyangie.sagapatternspringboot.aggregates.aggregates.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CreditCardProcessorRemoteService ccpRemoteService;

    @Override
    public PaymentDTO processPayment(PaymentDTO paymentDTO) {
        log.info("Processing payment: {}", paymentDTO);
        ccpRemoteService.process(paymentDTO.getPaymentAccountNumber(), paymentDTO.getPaymentTotalPrice());
        PaymentEntity paymentEntity =  PaymentEntity.builder()
                .paymentAccountNumber(paymentDTO.getPaymentAccountNumber())
                .paymentTotalPrice(paymentDTO.getPaymentTotalPrice())
                .paymentIsDefault(paymentDTO.isPaymentIsDefault())
                .paymentType(paymentDTO.getPaymentType())
                .paymentExpirationDate(paymentDTO.getPaymentExpirationDate())
                .userId(paymentDTO.getUserId())
                .paymentProvider(paymentDTO.getPaymentProvider())
                .shopOrderId(paymentDTO.getShopOrderId())
                .build();
        PaymentEntity paymentSaved = paymentRepository.save(paymentEntity);
        log.info("Payment processed: {}", paymentSaved);
        return paymentMapper.paymentDTO(paymentSaved);
    }

    @Override
    public ResponseListPageablePayments listAllPaymentsPageable(int pageNumber, int pageSize, String orderBy, String sortDir) {
        log.info("Searching all payments with the following parameters: {}", Constants.parametersForLogger(pageNumber, pageSize, orderBy, sortDir));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(orderBy).ascending() : Sort.by(orderBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<PaymentEntity> paymentEntityPage = paymentRepository.findAll(pageable);
        if(paymentEntityPage.isEmpty()) {
            log.info("No payments found");
        }
        return ResponseListPageablePayments.builder()
                .payments(paymentEntityPage.getContent().stream().map(paymentMapper::paymentDTO).toList())
                .pageNumber(paymentEntityPage.getNumber())
                .totalElements(paymentEntityPage.getTotalElements())
                .totalPages(paymentEntityPage.getTotalPages())
                .pageSize(paymentEntityPage.getSize())
                .end(paymentEntityPage.isLast())
                .build();
    }

    @Override
    public PaymentDTO findPaymentByShopOrderId(Long shopOrderId) {
        log.info("Searching payment with ID: {}", shopOrderId);
        PaymentEntity paymentEntity = paymentRepository.findByShopOrderId(shopOrderId)
                .orElseThrow(() -> {
                    log.error("Payment not found");
                    return new AppExceptionNotFound("Payment not found");
                });
        log.info("Payment found: {}", paymentEntity);
        return paymentMapper.paymentDTO(paymentEntity);
    }
}
