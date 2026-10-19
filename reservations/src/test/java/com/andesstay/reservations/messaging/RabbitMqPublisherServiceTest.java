package com.andesstay.reservations.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class RabbitMqPublisherServiceTest {

	private RabbitTemplate rabbitTemplate;
	private RabbitMqPublisherService publisherService;

	@BeforeEach
	void setUp() {
		rabbitTemplate = mock(RabbitTemplate.class);
		publisherService = new RabbitMqPublisherService(rabbitTemplate);
		ReflectionTestUtils.setField(publisherService, "cmdExchange", "cmd.direct");
		ReflectionTestUtils.setField(publisherService, "emailRoutingKey", "email.send");
		ReflectionTestUtils.setField(publisherService, "housekeepingRoutingKey", "housekeeping.ticket");
		ReflectionTestUtils.setField(publisherService, "voucherRoutingKey", "voucher.gen");
	}

	private ReservationNotification sampleNotification() {
		return new ReservationNotification("7", "Juan Perez", "juan@example.com", "10",
				"trace-1", "correlation-1");
	}

	@Test
	void publicaEnvelopeEnLosTresRoutingKeysConPayloadEsperado() {
		ReservationNotification notification = sampleNotification();

		publisherService.publishEmailConfirmation(notification);
		publisherService.publishHousekeepingTicket(notification);
		publisherService.publishVoucherGeneration(notification);

		ArgumentCaptor<NotificationEnvelope> emailCaptor = ArgumentCaptor.forClass(NotificationEnvelope.class);
		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("email.send"), emailCaptor.capture());
		NotificationEnvelope email = emailCaptor.getValue();
		assertEquals("email.send", email.type());
		assertNotNull(email.eventId());
		assertNotNull(email.timestamp());
		assertEquals("trace-1", email.traceId());
		assertEquals("correlation-1", email.correlationId());
		assertEquals("7", email.payload().get("reservationId"));
		assertEquals("Juan Perez", email.payload().get("guestName"));
		assertEquals("CONFIRMATION", email.payload().get("kind"));
		assertEquals("EMAIL", email.payload().get("channel"));
		assertEquals("juan@example.com", email.payload().get("guestEmail"));

		ArgumentCaptor<NotificationEnvelope> housekeepingCaptor = ArgumentCaptor.forClass(NotificationEnvelope.class);
		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("housekeeping.ticket"), housekeepingCaptor.capture());
		NotificationEnvelope housekeeping = housekeepingCaptor.getValue();
		assertEquals("housekeeping.ticket", housekeeping.type());
		assertEquals("7", housekeeping.payload().get("reservationId"));
		assertEquals("10", housekeeping.payload().get("unitId"));
		assertEquals("PREPARATION", housekeeping.payload().get("kind"));

		ArgumentCaptor<NotificationEnvelope> voucherCaptor = ArgumentCaptor.forClass(NotificationEnvelope.class);
		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("voucher.gen"), voucherCaptor.capture());
		NotificationEnvelope voucher = voucherCaptor.getValue();
		assertEquals("voucher.gen", voucher.type());
		assertEquals("7", voucher.payload().get("reservationId"));
		assertEquals("Juan Perez", voucher.payload().get("guestName"));
		assertEquals("VOUCHER", voucher.payload().get("document"));
	}

	@Test
	void omiteMensajeDeEmailCuandoGuestEmailEstaAusente() {
		ReservationNotification notification = new ReservationNotification("7", "Juan Perez", "  ", "10",
				"trace-1", "correlation-1");

		publisherService.publishEmailConfirmation(notification);

		verify(rabbitTemplate, never()).convertAndSend(eq("cmd.direct"), eq("email.send"), any(Object.class));
	}

	@Test
	void toleraFalloDeConexionSinPropagarExcepcion() {
		doThrow(new AmqpException("broker no disponible"))
				.when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

		assertDoesNotThrow(() -> publisherService.publishEmailConfirmation(sampleNotification()));
	}

	@Test
	void toleraFalloSincronoGenericoSinPropagarExcepcion() {
		doThrow(new IllegalStateException("fallo inesperado"))
				.when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

		assertDoesNotThrow(() -> publisherService.publishHousekeepingTicket(sampleNotification()));
		assertDoesNotThrow(() -> publisherService.publishVoucherGeneration(sampleNotification()));
	}
}
