package com.andesstay.reservations.messaging;

import java.util.UUID;

import com.andesstay.reservations.config.RabbitMessagingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
		RabbitMessagingProperties properties = new RabbitMessagingProperties();
		RabbitMessagingProperties.Exchange exchange = new RabbitMessagingProperties.Exchange();
		exchange.setCmd("cmd.direct");
		properties.setExchange(exchange);
		RabbitMessagingProperties.Routing routing = new RabbitMessagingProperties.Routing();
		routing.setEmail("email.send");
		routing.setHousekeeping("housekeeping.ticket");
		routing.setVoucher("voucher.gen");
		properties.setRouting(routing);
		publisherService = new RabbitMqPublisherService(rabbitTemplate, properties);
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

	@Test
	void publicaCabinReadyConRoutingKeyYPayloadAcordados() {
		publisherService.publishCabinReady(sampleNotification());

		ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("cabin.ready"), captor.capture());
		NotificationEnvelope envelope = captor.getValue();
		assertEquals("cabin.ready", envelope.type());
		assertNotNull(envelope.eventId());
		assertEquals("trace-1", envelope.traceId());
		assertEquals("correlation-1", envelope.correlationId());
		assertEquals("7", envelope.payload().get("reservationId"));
		assertEquals("Juan Perez", envelope.payload().get("guestName"));
		assertEquals("juan@example.com", envelope.payload().get("guestEmail"));
		assertEquals("10", envelope.payload().get("unitId"));
		assertEquals("CABIN_READY", envelope.payload().get("kind"));
		assertEquals("EMAIL", envelope.payload().get("channel"));
	}

	@Test
	void permiteCambiarRoutingKeyDeCabinReady() {
		RabbitMessagingProperties properties = new RabbitMessagingProperties();
		RabbitMessagingProperties.Exchange exchange = new RabbitMessagingProperties.Exchange();
		exchange.setCmd("cmd.direct");
		properties.setExchange(exchange);
		RabbitMessagingProperties.Routing routing = new RabbitMessagingProperties.Routing();
		routing.setCabinReady("cabin.listo");
		properties.setRouting(routing);
		RabbitMqPublisherService configurablePublisher = new RabbitMqPublisherService(rabbitTemplate, properties);

		configurablePublisher.publishCabinReady(sampleNotification());

		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq("cabin.listo"), any(Object.class));
	}

	@Test
	void toleraFalloAlPublicarCabinReadySinPropagarExcepcion() {
		doThrow(new AmqpException("broker no disponible"))
				.when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

		assertDoesNotThrow(() -> publisherService.publishCabinReady(sampleNotification()));
	}

	@Test
	void serializaCabinReadyConSobreCompletoYPayloadAnidado() {
		String cabinReadyRoutingKey = "cabin.listo.contract";
		RabbitMessagingProperties properties = new RabbitMessagingProperties();
		RabbitMessagingProperties.Exchange exchange = new RabbitMessagingProperties.Exchange();
		exchange.setCmd("cmd.direct");
		properties.setExchange(exchange);
		RabbitMessagingProperties.Routing routing = new RabbitMessagingProperties.Routing();
		routing.setCabinReady(cabinReadyRoutingKey);
		properties.setRouting(routing);
		RabbitMqPublisherService configurablePublisher = new RabbitMqPublisherService(rabbitTemplate, properties);

		ReservationNotification notification = new ReservationNotification("7", "Juan Perez", null, "10",
				"trace-1", "correlation-1");
		configurablePublisher.publishCabinReady(notification);

		ArgumentCaptor<NotificationEnvelope> captor = ArgumentCaptor.forClass(NotificationEnvelope.class);
		verify(rabbitTemplate).convertAndSend(eq("cmd.direct"), eq(cabinReadyRoutingKey), captor.capture());
		NotificationEnvelope envelope = captor.getValue();
		assertEquals(cabinReadyRoutingKey, envelope.type());

		JsonNode root = assertDoesNotThrow(() -> {
			Message message = new JacksonJsonMessageConverter().toMessage(envelope, new MessageProperties());
			return new ObjectMapper().readTree(message.getBody());
		});

		assertEquals(cabinReadyRoutingKey, root.get("type").asText());
		assertDoesNotThrow(() -> UUID.fromString(root.get("eventId").asText()));
		assertTrue(root.get("timestamp").isTextual());
		assertEquals("trace-1", root.get("traceId").asText());
		assertEquals("correlation-1", root.get("correlationId").asText());

		JsonNode payload = root.get("payload");
		assertTrue(payload.isObject());
		assertEquals("7", payload.get("reservationId").asText());
		assertEquals("Juan Perez", payload.get("guestName").asText());
		assertEquals("10", payload.get("unitId").asText());
		assertEquals("CABIN_READY", payload.get("kind").asText());
		assertEquals("EMAIL", payload.get("channel").asText());
		JsonNode guestEmail = payload.get("guestEmail");
		assertTrue(guestEmail == null || guestEmail.isNull());
		assertTrue(root.get("reservationId") == null || root.get("reservationId").isMissingNode());
	}
}
