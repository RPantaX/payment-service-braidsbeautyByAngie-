package com.braidsbeautybyangie.paymentservice.repository;

import com.braidsbeautybyangie.paymentservice.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByShopOrderId(Long shopOrderId);
}
