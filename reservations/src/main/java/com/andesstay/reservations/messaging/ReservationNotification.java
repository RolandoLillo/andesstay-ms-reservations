package com.andesstay.reservations.messaging;

public record ReservationNotification(String reservationId, String guestName, String guestEmail, String unitId,
		String traceId, String correlationId) {
}
