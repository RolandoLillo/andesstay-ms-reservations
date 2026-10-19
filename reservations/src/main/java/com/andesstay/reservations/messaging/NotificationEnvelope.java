package com.andesstay.reservations.messaging;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationEnvelope(String type, String eventId, Instant timestamp, String traceId,
		String correlationId, Map<String, Object> payload) {

	public static NotificationEnvelope create(String type, String traceId, String correlationId,
			Map<String, Object> payload) {
		return new NotificationEnvelope(type, UUID.randomUUID().toString(), Instant.now(), traceId, correlationId,
				payload);
	}
}
