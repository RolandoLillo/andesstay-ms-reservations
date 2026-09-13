package com.andesstay.reservations.dto;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservationResponse(Long id, String customerUserId, Long unitId, LocalDate checkInDate,
		LocalDate checkOutDate, BigDecimal amount, ReservationStatus status) {

	public static ReservationResponse from(Reservation reservation) {
		return new ReservationResponse(reservation.getId(), reservation.getCustomerUserId(), reservation.getUnitId(),
				reservation.getCheckInDate(), reservation.getCheckOutDate(), reservation.getAmount(),
				reservation.getStatus());
	}
}