package com.andesstay.reservations.event;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationEventProducerTest {

	private Reservation sampleReservation() {
		return Reservation.builder()
				.id(7L)
				.customerUserId("user-1")
				.unitId(10L)
				.checkInDate(LocalDate.of(2026, 10, 15))
				.checkOutDate(LocalDate.of(2026, 10, 20))
				.amount(new BigDecimal("120000"))
				.status(ReservationStatus.CREADA)
				.unitName("Cabana A")
				.guests(2)
				.customerName("Juan Perez")
				.build();
	}

	@Test
	void serializaElEventoComoJsonConLosCamposEsperados() {
		ReservationEvent event = ReservationEvent.created(sampleReservation(), "user-1");

		byte[] payload = new JacksonJsonSerializer<ReservationEvent>().serialize("7", event);
		String json = new String(payload, StandardCharsets.UTF_8);

		assertTrue(json.contains("\"eventId\":\""), json);
		assertTrue(json.contains("\"occurredAt\":\""), json);
		assertTrue(json.contains("\"actor\":\"user-1\""), json);
		assertTrue(json.contains("\"action\":\"RESERVATION_CREATED\""), json);
		assertTrue(json.contains("\"resourceType\":\"RESERVATION\""), json);
		assertTrue(json.contains("\"resourceId\":7"), json);
		assertTrue(json.contains("\"newStatus\":\"CREADA\""), json);
		assertTrue(json.contains("\"previousStatus\":null"), json);
		assertTrue(json.contains("\"unitName\":\"Cabana A\""), json);
	}

	@Test
	void serializaElCambioDeEstadoConStatusAnteriorYNuevo() {
		Reservation reservation = sampleReservation();
		reservation.setStatus(ReservationStatus.CONFIRMADA);

		byte[] payload = new JacksonJsonSerializer<ReservationEvent>()
				.serialize("7", ReservationEvent.statusChanged(reservation, ReservationStatus.CREADA, "system"));
		String json = new String(payload, StandardCharsets.UTF_8);

		assertTrue(json.contains("\"action\":\"RESERVATION_STATUS_CHANGED\""), json);
		assertTrue(json.contains("\"previousStatus\":\"CREADA\""), json);
		assertTrue(json.contains("\"newStatus\":\"CONFIRMADA\""), json);
		assertTrue(json.contains("\"actor\":\"system\""), json);
	}

	@SuppressWarnings("unchecked")
	@Test
	void publishNoPropagaExcepcionesCuandoElBrokerNoResponde() {
		KafkaTemplate<String, ReservationEvent> kafkaTemplate = mock(KafkaTemplate.class);
		when(kafkaTemplate.send(anyString(), anyString(), any(ReservationEvent.class)))
				.thenThrow(new IllegalStateException("broker no disponible"));

		ReservationEventProducer producer = new ReservationEventProducer(kafkaTemplate);
		ReservationEvent event = ReservationEvent.created(sampleReservation(), "user-1");

		assertDoesNotThrow(() -> producer.publish(event));
	}

	@SuppressWarnings("unchecked")
	@Test
	void publishNoPropagaExcepcionesCuandoElEnvioFallaDeFormaAsincrona() {
		KafkaTemplate<String, ReservationEvent> kafkaTemplate = mock(KafkaTemplate.class);
		when(kafkaTemplate.send(anyString(), anyString(), any(ReservationEvent.class)))
				.thenReturn(CompletableFuture.failedFuture(new RuntimeException("fallo en el envio")));

		ReservationEventProducer producer = new ReservationEventProducer(kafkaTemplate);
		ReservationEvent event = ReservationEvent.created(sampleReservation(), "user-1");

		assertDoesNotThrow(() -> producer.publish(event));
	}
}
