package com.andesstay.reservations.event;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class ReservationEventProducer {

	private static final Logger log = LoggerFactory.getLogger(ReservationEventProducer.class);

	private final KafkaTemplate<String, ReservationEvent> kafkaTemplate;

	public void publish(ReservationEvent event) {
		try {
			CompletableFuture<SendResult<String, ReservationEvent>> future = kafkaTemplate.send(
					ReservationEvent.RESERVATIONS_TOPIC, String.valueOf(event.resourceId()), event);
			future.whenComplete((result, ex) -> {
				if (ex != null) {
					log.error("No se pudo publicar el evento {} de la reserva {} en el topico {}",
							event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC, ex);
					return;
				}
				log.info("Evento {} de la reserva {} publicado en el topico {} (particion {})",
						event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC,
						result.getRecordMetadata().partition());
			});
		} catch (Exception ex) {
			log.error("No se pudo encolar el evento {} de la reserva {} hacia el topico {}",
					event.action(), event.resourceId(), ReservationEvent.RESERVATIONS_TOPIC, ex);
		}
	}
}
