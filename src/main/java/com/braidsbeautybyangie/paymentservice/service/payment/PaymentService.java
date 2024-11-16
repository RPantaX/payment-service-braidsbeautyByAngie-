package com.braidsbeautybyangie.paymentservice.service.payment;

import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import com.braidsbeautybyangie.paymentservice.model.response.ResponseListPageablePayments;

public interface PaymentService {
    PaymentDTO processPayment(PaymentDTO paymentDTO);
    ResponseListPageablePayments listAllPaymentsPageable(int pageNumber, int pageSize, String orderBy, String sortDir);
}
