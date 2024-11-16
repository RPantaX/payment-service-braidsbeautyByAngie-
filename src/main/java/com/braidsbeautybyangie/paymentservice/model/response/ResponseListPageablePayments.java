package com.braidsbeautybyangie.paymentservice.model.response;

import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import lombok.*;

import java.util.List;
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ResponseListPageablePayments {
    private List<PaymentDTO> payments;
    private int pageNumber;
    private int pageSize;
    private int totalPages;
    private long totalElements;
    private boolean end;
}
