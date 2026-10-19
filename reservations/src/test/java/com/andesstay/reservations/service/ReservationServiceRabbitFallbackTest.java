package com.andesstay.reservations.service;

import com.andesstay.reservations.domain.Reservation;
import com.andesstay.reservations.domain.ReservationStatus;
import com.andesstay.reservations.dto.CreateReservationRequest;
import com.andesstay.reservations.event.ReservationEventProducer;
import com.andesstay.reservations.messaging.RabbitMqPublisherService;
import com.andesstay.reservations.messaging.ReservationNotification;
import com.andesstay.reservations.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReservationServiceRabbitFallbackTest {

	private ReservationRepository reservationRepository;
	private ReservationEventProducer reservationEventProducer;
	private RabbitMqPublisherService rabbitMqPublisherService;
	private ReservationService reservationService;

	@BeforeEach
	void setUp() {
		reservationRepository = mock(ReservationRepository.class);
		reservationEventProducer = mock(ReservationEventProducer.class);
		rabbitMqPublisherService = spy(createPublisher(mock(RabbitTemplate.class)));
		reservationService = new ReservationService(reservationRepository, reservationEventProducer,
				rabbitMqPublisherService);
	}

	private RabbitMqPublisherService createPublisher(RabbitTemplate rabbitTemplate) {
		RabbitMqPublisherService service = new RabbitMqPublisherService(rabbitTemplate);
		ReflectionTestUtils.setField(service, "cmdExchange", "cmd.direct");
		ReflectionTestUtils.setField(service, "emailRoutingKey", "email.send");
		ReflectionTestUtils.setField(service, "housekeepingRoutingKey", "housekeeping.ticket");
		ReflectionTestUtils.setField(service, "voucherRoutingKey", "voucher.gen");
		return service;
	}

	private Reservation sampleReservation(Long id, ReservationStatus status) {
		return Reservation.builder()
				.id(id)
				.customerUserId("user-1")
				.unitId(10L)
				.checkInDate(LocalDate.of(2026, 10, 15))
				.checkOutDate(LocalDate.of(2026, 10, 20))
				.amount(new BigDecimal("120000"))
				.status(status)
				.unitName("Cabana A")
				.guests(2)
				.customerName("Juan Perez")
				.guestEmail("juan@example.com")
				.build();
	}

	private CreateReservationRequest sampleRequest() {
		return new CreateReservationRequest("user-1", 10L,
				LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20),
				new BigDecimal("120000"), "Cabana A", 2, "Juan Perez", "juan@example.com");
	}

	@Test
	void crearReservaNoPublicaNotificaciones() {
		Reservation saved = sampleReservation(7L, ReservationStatus.CREADA);
		when(reservationRepository.save(any(Reservation.class))).thenReturn(saved);

		Reservation result = reservationService.create(sampleRequest());

		assertEquals(7L, result.getId());
		assertEquals(ReservationStatus.CREADA, result.getStatus());
		verify(reservationEventProducer).publish(any());
		verifyNoInteractions(rabbitMqPublisherService);
	}

	@Test
	void confirmarReservaPublicaNotificacionesConDatosDeLaReserva() {
		Reservation creada = sampleReservation(7L, ReservationStatus.CREADA);
		Reservation confirmada = sampleReservation(7L, ReservationStatus.CONFIRMADA);
		when(reservationRepository.findById(anyLong())).thenReturn(java.util.Optional.of(creada));
		when(reservationRepository.save(any(Reservation.class))).thenReturn(confirmada);

		Reservation result = reservationService.updateStatus(7L, ReservationStatus.CONFIRMADA, "admin-1");

		assertEquals(ReservationStatus.CONFIRMADA, result.getStatus());
		ArgumentCaptor<ReservationNotification> captor = ArgumentCaptor.forClass(ReservationNotification.class);
		verify(rabbitMqPublisherService).publishEmailConfirmation(captor.capture());
		verify(rabbitMqPublisherService).publishHousekeepingTicket(captor.capture());
		verify(rabbitMqPublisherService).publishVoucherGeneration(captor.capture());
		ReservationNotification notification = captor.getValue();
		assertEquals("7", notification.reservationId());
		assertEquals("Juan Perez", notification.guestName());
		assertEquals("juan@example.com", notification.guestEmail());
		assertEquals("10", notification.unitId());
	}

	@Test
	void confirmarReservaNoFallaCuandoRabbitMQEstaCaido() {
		Reservation creada = sampleReservation(7L, ReservationStatus.CREADA);
		Reservation confirmada = sampleReservation(7L, ReservationStatus.CONFIRMADA);
		when(reservationRepository.findById(anyLong())).thenReturn(java.util.Optional.of(creada));
		when(reservationRepository.save(any(Reservation.class))).thenReturn(confirmada);
		RabbitTemplate failingTemplate = mock(RabbitTemplate.class);
		doThrow(new AmqpException("broker caido"))
				.when(failingTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
		ReservationService serviceWithBrokenRabbit = new ReservationService(reservationRepository,
				reservationEventProducer, createPublisher(failingTemplate));

		Reservation result = assertDoesNotThrow(
				() -> serviceWithBrokenRabbit.updateStatus(7L, ReservationStatus.CONFIRMADA, "admin-1"));

		assertEquals(ReservationStatus.CONFIRMADA, result.getStatus());
		verify(reservationEventProducer).publish(any());
	}

	@Test
	void actualizarEstadoANoConfirmadaNoPublicaNotificaciones() {
		Reservation creada = sampleReservation(7L, ReservationStatus.CREADA);
		Reservation cancelada = sampleReservation(7L, ReservationStatus.CANCELADA);
		when(reservationRepository.findById(anyLong())).thenReturn(java.util.Optional.of(creada));
		when(reservationRepository.save(any(Reservation.class))).thenReturn(cancelada);

		reservationService.updateStatus(7L, ReservationStatus.CANCELADA, "admin-1");

		verifyNoInteractions(rabbitMqPublisherService);
	}
}
