package com.braidsbeautybyangie.paymentservice.service.payment.impl;


import com.braidsbeautybyangie.coreservice.aggregates.Constants;
import com.braidsbeautybyangie.paymentservice.model.PaymentEntity;
import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.model.mapper.PaymentMapper;
import com.braidsbeautybyangie.paymentservice.model.response.ResponseListPageablePayments;
import com.braidsbeautybyangie.paymentservice.repository.PaymentRepository;
import com.braidsbeautybyangie.paymentservice.service.creditCard.CreditCardProcessorRemoteService;
import com.braidsbeautybyangie.paymentservice.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CreditCardProcessorRemoteService ccpRemoteService;
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Override
    public PaymentDTO processPayment(PaymentDTO paymentDTO) {
        LOGGER.info("Processing payment: {}", paymentDTO);
        ccpRemoteService.process(paymentDTO.getPaymentAccountNumber(), paymentDTO.getPaymentTotalPrice());
        PaymentEntity paymentEntity =  PaymentEntity.builder()
                .paymentAccountNumber(paymentDTO.getPaymentAccountNumber())
                .paymentTotalPrice(paymentDTO.getPaymentTotalPrice())
                .paymentIsDefault(paymentDTO.isPaymentIsDefault())
                .paymentType(paymentDTO.getPaymentType())
                .paymentExpirationDate(paymentDTO.getPaymentExpirationDate())
                .userId(paymentDTO.getUserId())
                .paymentProvider(paymentDTO.getPaymentProvider())
                .build();
        PaymentEntity paymentSaved = paymentRepository.save(paymentEntity);
        LOGGER.info("Payment processed: {}", paymentSaved);
        return paymentMapper.paymentDTO(paymentSaved);
    }

    @Override
    public ResponseListPageablePayments listAllPaymentsPageable(int pageNumber, int pageSize, String orderBy, String sortDir) {
        LOGGER.info("Searching all payments with the following parameters: {}", Constants.parametersForLogger(pageNumber, pageSize, orderBy, sortDir));
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(orderBy).ascending() : Sort.by(orderBy).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<PaymentEntity> paymentEntityPage = paymentRepository.findAll(pageable);

        return ResponseListPageablePayments.builder()
                .payments(paymentEntityPage.getContent().stream().map(paymentMapper::paymentDTO).toList())
                .pageNumber(paymentEntityPage.getNumber())
                .totalElements(paymentEntityPage.getTotalElements())
                .totalPages(paymentEntityPage.getTotalPages())
                .pageSize(paymentEntityPage.getSize())
                .end(paymentEntityPage.isLast())
                .build();
    }
}
