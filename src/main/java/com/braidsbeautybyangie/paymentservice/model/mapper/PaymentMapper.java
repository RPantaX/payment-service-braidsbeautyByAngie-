package com.braidsbeautybyangie.paymentservice.model.mapper;

import com.braidsbeautybyangie.paymentservice.model.PaymentEntity;
import com.braidsbeautybyangie.paymentservice.model.dto.PaymentDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
public class PaymentMapper {

    private static final ModelMapper modelMapper = new ModelMapper();

    public PaymentEntity toEntity(PaymentDTO paymentDTO){
        return modelMapper.map(paymentDTO, PaymentEntity.class);
    }
    public PaymentDTO paymentDTO(PaymentEntity paymentEntity){
        return modelMapper.map(paymentEntity, PaymentDTO.class);
    }
}
