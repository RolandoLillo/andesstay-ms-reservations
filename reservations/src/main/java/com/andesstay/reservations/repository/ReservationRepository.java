package com.andesstay.reservations.repository;

import com.andesstay.reservations.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	List<Reservation> findByCustomerUserId(String customerUserId);
}