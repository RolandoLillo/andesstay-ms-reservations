package com.andesstay.reservations.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReservationRequest(String customerUserId, Long unitId, LocalDate checkInDate,
		LocalDate checkOutDate, BigDecimal amount, String unitName, Integer guests, String customerName,
		String guestEmail) {

	public CreateReservationRequest(String customerUserId, Long unitId, LocalDate checkInDate,
			LocalDate checkOutDate, BigDecimal amount, String unitName, Integer guests, String customerName) {
		this(customerUserId, unitId, checkInDate, checkOutDate, amount, unitName, guests, customerName, null);
	}
}
