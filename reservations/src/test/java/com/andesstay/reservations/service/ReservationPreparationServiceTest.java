package com.andesstay.reservations.service;

import com.andesstay.reservations.config.RabbitMessagingProperties;
import com.andesstay.reservations.domain.PreparationStatus;
import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;
import com.andesstay.reservations.event.ReservationEventProducer;
import com.andesstay.reservations.exception.InvalidPreparationTransitionException;
import com.andesstay.reservations.messaging.RabbitMqPublisherService;
import com.andesstay.reservations.messaging.ReservationNotification;
import com.andesstay.reservations.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReservationPreparationServiceTest {

	private ReservationRepository reservationRepository;
	private ReservationEventProducer reservationEventProducer;
	private RabbitMqPublisherService rabbitMqPublisherService;
	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		reservationRepository = mock(ReservationRepository.class);
		reservationEventProducer = mock(ReservationEventProducer.class);
		rabbitMqPublisherService = mock(RabbitMqPublisherService.class);
		reservationService = new ReservationService(reservationRepository, reservationEventProducer,
				rabbitMqPublisherService);
	}

	private Reservation sampleReservation(ReservationStatus status, PreparationStatus preparationStatus) {
		return Reservation.builder()
				.id(7L)
				.customerUserId("user-1")
				.unitId(10L)
				.checkInDate(LocalDate.of(2026, 10, 15))
				.checkOutDate(LocalDate.of(2026, 10, 20))
				.amount(new BigDecimal("120000"))
				.status(status)
				.preparationStatus(preparationStatus)
				.unitName("Cabana A")
				.guests(2)
				.customerName("Juan Perez")
				.guestEmail("juan@example.com")
				.build();
	}

	private void givenStored(Reservation reservation) {
		when(reservationRepository.findById(7L)).thenReturn(Optional.of(reservation));
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void confirmarDesdeCreadaMarcaPreparationComoPending() {
		givenStored(sampleReservation(ReservationStatus.CREADA, PreparationStatus.NONE));

		Reservation result = reservationService.updateStatus(7L, ReservationStatus.CONFIRMADA, "admin-1");

		assertEquals(PreparationStatus.PENDING, result.getPreparationStatus());
	}

	@Test
	void confirmarRepetidoNoReiniciaPreparationYaAvanzada() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.READY));

		Reservation result = reservationService.updateStatus(7L, ReservationStatus.CONFIRMADA, "admin-1");

		assertEquals(PreparationStatus.READY, result.getPreparationStatus());
	}

	@Test
	void pasarAInProgressNoPublicaCabinReady() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.PENDING));

		Reservation result = reservationService.updatePreparation(7L, PreparationStatus.IN_PROGRESS, "admin-1");

		assertEquals(PreparationStatus.IN_PROGRESS, result.getPreparationStatus());
		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void pasarAReadyPublicaCabinReadyConDatosDeLaReserva() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.PENDING));

		Reservation result = reservationService.updatePreparation(7L, PreparationStatus.READY, "admin-1");

		assertEquals(PreparationStatus.READY, result.getPreparationStatus());
		ArgumentCaptor<ReservationNotification> captor = ArgumentCaptor.forClass(ReservationNotification.class);
		verify(rabbitMqPublisherService).publishCabinReady(captor.capture());
		ReservationNotification notification = captor.getValue();
		assertEquals("7", notification.reservationId());
		assertEquals("Juan Perez", notification.guestName());
		assertEquals("juan@example.com", notification.guestEmail());
		assertEquals("10", notification.unitId());
		assertEquals("admin-1", notification.traceId());
	}

	@Test
	void repetirReadyNoPublicaNuevamente() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.READY));

		Reservation result = reservationService.updatePreparation(7L, PreparationStatus.READY, "admin-1");

		assertEquals(PreparationStatus.READY, result.getPreparationStatus());
		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void transicionInvalidaDePreparationLanzaConflicto() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.NONE));

		assertThrows(InvalidPreparationTransitionException.class,
				() -> reservationService.updatePreparation(7L, PreparationStatus.IN_PROGRESS, "admin-1"));
		verify(rabbitMqPublisherService, never()).publishCabinReady(any(ReservationNotification.class));
	}

	@Test
	void reservaNoConfirmadaNoAceptaPreparation() {
		givenStored(sampleReservation(ReservationStatus.CREADA, PreparationStatus.NONE));

		assertThrows(InvalidPreparationTransitionException.class,
				() -> reservationService.updatePreparation(7L, PreparationStatus.IN_PROGRESS, "admin-1"));
	}

	@Test
	void estadoDeEntradaDistintoDeInProgressReadyEsRechazado() {
		assertThrows(IllegalArgumentException.class,
				() -> reservationService.updatePreparation(7L, PreparationStatus.PENDING, "admin-1"));
		assertThrows(IllegalArgumentException.class,
				() -> reservationService.updatePreparation(7L, PreparationStatus.NONE, "admin-1"));
	}

	@Test
	void falloDelRabbitNoRompeLaTransicionAReady() {
		givenStored(sampleReservation(ReservationStatus.CONFIRMADA, PreparationStatus.PENDING));
		RabbitTemplate failingTemplate = mock(RabbitTemplate.class);
		doThrow(new AmqpException("broker caido"))
				.when(failingTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
		ReservationService serviceWithBrokenRabbit = new ReservationService(reservationRepository,
				reservationEventProducer, createPublisher(failingTemplate));

		Reservation result = assertDoesNotThrow(
				() -> serviceWithBrokenRabbit.updatePreparation(7L, PreparationStatus.READY, null));

		assertEquals(PreparationStatus.READY, result.getPreparationStatus());
	}

	private RabbitMqPublisherService createPublisher(RabbitTemplate rabbitTemplate) {
		RabbitMessagingProperties properties = new RabbitMessagingProperties();
		RabbitMessagingProperties.Exchange exchange = new RabbitMessagingProperties.Exchange();
		exchange.setCmd("cmd.direct");
		properties.setExchange(exchange);
		RabbitMessagingProperties.Routing routing = new RabbitMessagingProperties.Routing();
		routing.setEmail("email.send");
		routing.setHousekeeping("housekeeping.ticket");
		routing.setVoucher("voucher.gen");
		properties.setRouting(routing);
		return new RabbitMqPublisherService(rabbitTemplate, properties);
	}
}
