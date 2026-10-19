package com.andesstay.reservations.event;

import com.andesstay.reservations.exception.EventPublishException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class ReservationEventProducer {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventProducer.class);

	private static final long PUBLISH_TIMEOUT_SECONDS = 5;

	private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;

	public void publish(ReservationEvent event) {
		try {
			SendResult<String, ReservationEvent> result = kafkaTemplate.send(
					ReservationEvent.RESERVATIONS_TOPIC, String.valueOf(event.resourceId()), event)
					.get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			log.info("Evento {} de la reserva {} publicado en el topico {} (particion {})",
					event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC,
					result.getRecordMetadata().partition());
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			log.error("Interrumpido al publicar el evento {} de la reserva {} en el topico {}",
					event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC, ex);
			throw new EventPublishException("Publicacion interrumpida en " + ReservationEvent.RESERVATIONS_TOPIC, ex);
		} catch (ExecutionException | TimeoutException ex) {
			log.error("No se pudo publicar el evento {} de la reserva {} en el topico {}",
					event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC, ex);
			throw new EventPublishException("No se pudo publicar en " + ReservationEvent.RESERVATIONS_TOPIC, ex);
		} catch (RuntimeException ex) {
			log.error("No se pudo encolar el evento {} de la reserva {} hacia el topico {}",
					event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC, ex);
			throw new EventPublishException("No se pudo encolar en " + ReservationEvent.RESERVATIONS_TOPIC, ex);
		}
	}
}
