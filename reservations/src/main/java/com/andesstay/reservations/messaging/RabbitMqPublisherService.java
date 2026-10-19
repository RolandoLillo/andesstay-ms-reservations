package com.andesstay.reservations.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqPublisherService {

	private static final String EMAIL_KIND_CONFIRMATION = "CONFIRMATION";

	private static final String EMAIL_CHANNEL = "EMAIL";

	private static final String HOUSEKEEPING_KIND_PREPARATION = "PREPARATION";

	private static final String VOUCHER_DOCUMENT = "VOUCHER";

	private final RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.exchange.cmd:cmd.direct}")
	private String cmdExchange;

	@Value("${rabbitmq.routing.email:email.send}")
	private String emailRoutingKey;

	@Value("${rabbitmq.routing.housekeeping:housekeeping.ticket}")
	private String housekeepingRoutingKey;

	@Value("${rabbitmq.routing.voucher:voucher.gen}")
	private String voucherRoutingKey;

	public void publishEmailConfirmation(ReservationNotification notification) {
		if (isBlank(notification.guestEmail())) {
			log.warn("Se omite '{}' para reservationId={}: guestEmail ausente",
					emailRoutingKey, notification.reservationId());
			return;
		}
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationId", notification.reservationId());
		payload.put("guestName", notification.guestName());
		payload.put("kind", EMAIL_KIND_CONFIRMATION);
		payload.put("channel", EMAIL_CHANNEL);
		payload.put("guestEmail", notification.guestEmail());
		publish(NotificationEnvelope.create(emailRoutingKey, notification.traceId(), notification.correlationId(),
				payload));
	}

	public void publishHousekeepingTicket(ReservationNotification notification) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationId", notification.reservationId());
		payload.put("unitId", notification.unitId());
		payload.put("kind", HOUSEKEEPING_KIND_PREPARATION);
		publish(NotificationEnvelope.create(housekeepingRoutingKey, notification.traceId(),
				notification.correlationId(), payload));
	}

	public void publishVoucherGeneration(ReservationNotification notification) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationId", notification.reservationId());
		payload.put("guestName", notification.guestName());
		payload.put("document", VOUCHER_DOCUMENT);
		publish(NotificationEnvelope.create(voucherRoutingKey, notification.traceId(), notification.correlationId(),
				payload));
	}

	private void publish(NotificationEnvelope envelope) {
		try {
			rabbitTemplate.convertAndSend(cmdExchange, envelope.type(), envelope);
			log.info("Mensaje publicado en exchange={} type={} eventId={} reservationId={}",
					cmdExchange, envelope.type(), envelope.eventId(), envelope.payload().get("reservationId"));
		} catch (RuntimeException ex) {
			log.error("No se pudo publicar mensaje en exchange={} type={} eventId={}",
					cmdExchange, envelope.type(), envelope.eventId(), ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
