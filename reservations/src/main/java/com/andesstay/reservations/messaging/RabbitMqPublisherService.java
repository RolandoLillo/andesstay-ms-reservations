package com.andesstay.reservations.messaging;

import com.andesstay.reservations.config.RabbitMessagingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
	private final RabbitMessagingProperties rabbitMessagingProperties;

	private String getCmdExchange() {
		return rabbitMessagingProperties.getExchange();
	}

	private String getEmailRoutingKey() {
		return rabbitMessagingProperties.getRouting().getEmail();
	}

	private String getHousekeepingRoutingKey() {
		return rabbitMessagingProperties.getRouting().getHousekeeping();
	}

	private String getVoucherRoutingKey() {
		return rabbitMessagingProperties.getRouting().getVoucher();
	}

	public void publishEmailConfirmation(ReservationNotification notification) {
		String emailRoutingKey = getEmailRoutingKey();
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
		String housekeepingRoutingKey = getHousekeepingRoutingKey();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationId", notification.reservationId());
		payload.put("unitId", notification.unitId());
		payload.put("kind", HOUSEKEEPING_KIND_PREPARATION);
		publish(NotificationEnvelope.create(housekeepingRoutingKey, notification.traceId(),
				notification.correlationId(), payload));
	}

	public void publishVoucherGeneration(ReservationNotification notification) {
		String voucherRoutingKey = getVoucherRoutingKey();
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("reservationId", notification.reservationId());
		payload.put("guestName", notification.guestName());
		payload.put("document", VOUCHER_DOCUMENT);
		publish(NotificationEnvelope.create(voucherRoutingKey, notification.traceId(), notification.correlationId(),
				payload));
	}

	private void publish(NotificationEnvelope envelope) {
		try {
			String cmdExchange = getCmdExchange();
			rabbitTemplate.convertAndSend(cmdExchange, envelope.type(), envelope);
			log.info("Mensaje publicado en exchange={} type={} eventId={} reservationId={}",
					cmdExchange, envelope.type(), envelope.eventId(), envelope.payload().get("reservationId"));
		} catch (RuntimeException ex) {
			log.error("No se pudo publicar mensaje en exchange={} type={} eventId={}",
					getCmdExchange(), envelope.type(), envelope.eventId(), ex);
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
