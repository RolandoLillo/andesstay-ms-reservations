package com.andesstay.reservations.event;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record ReservationEvent(String eventId, Instant occurredAt, String actor, ReservationAction action,
		String resourceType, Long resourceId, Map<String, Object> detail) {

	public static final String RESERVATIONS_TOPIC = "reservations.events";

	public static final String RESOURCE_TYPE = "RESERVATION";

	public static ReservationEvent created(Reservation reservation, String actor) {
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("newStatus", reservation.getStatus().name());
		detail.put("previousStatus", null);
		detail.put("checkInDate", reservation.getCheckInDate().toString());
		detail.put("checkOutDate", reservation.getCheckOutDate().toString());
		detail.put("unitId", reservation.getUnitId());
		detail.put("unitName", reservation.getUnitName());
		detail.put("guests", reservation.getGuests());
		detail.put("amount", reservation.getAmount());
		return new ReservationEvent(UUID.randomUUID().toString(), Instant.now(), actor,
				ReservationAction.RESERVATION_CREATED, RESOURCE_TYPE, reservation.getId(), detail);
	}

	public static ReservationEvent statusChanged(Reservation reservation, ReservationStatus previousStatus,
			String actor) {
		Map<String, Object> detail = new LinkedHashMap<>();
		detail.put("previousStatus", previousStatus.name());
		detail.put("newStatus", reservation.getStatus().name());
		detail.put("unitId", reservation.getUnitId());
		return new ReservationEvent(UUID.randomUUID().toString(), Instant.now(), actor,
				ReservationAction.RESERVATION_STATUS_CHANGED, RESOURCE_TYPE, reservation.getId(), detail);
	}
}
