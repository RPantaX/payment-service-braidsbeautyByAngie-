package com.braidsbeautybyangie.paymentservice.repository;

import com.braidsbeautybyangie.paymentservice.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
}
