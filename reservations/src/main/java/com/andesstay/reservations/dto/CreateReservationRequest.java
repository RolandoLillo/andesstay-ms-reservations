package com.andesstay.reservations.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateReservationRequest(String customerUserId, Long unitId, LocalDate checkInDate,
		LocalDate checkOutDate, BigDecimal amount) {
}