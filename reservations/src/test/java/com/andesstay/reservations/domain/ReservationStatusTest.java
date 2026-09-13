package com.andesstay.reservations.domain;

import com.andesstay.reservations.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationStatusTest {

	@Test
	void creadaPuedeConfirmarse() {
		assertEquals(ReservationStatus.CONFIRMADA, ReservationStatus.CREADA.transit(ReservationStatus.CONFIRMADA));
	}

	@Test
	void creadaPuedeCancelarse() {
		assertEquals(ReservationStatus.CANCELADA, ReservationStatus.CREADA.transit(ReservationStatus.CANCELADA));
	}

	@Test
	void confirmadaPermiteCheckinPendiente() {
		assertEquals(ReservationStatus.CHECKIN_PENDIENTE,
				ReservationStatus.CONFIRMADA.transit(ReservationStatus.CHECKIN_PENDIENTE));
	}

	@Test
	void confirmadaPuedeCancelarse() {
		assertEquals(ReservationStatus.CANCELADA,
				ReservationStatus.CONFIRMADA.transit(ReservationStatus.CANCELADA));
	}

	@Test
	void checkinPendientePermiteEnEstadia() {
		assertEquals(ReservationStatus.EN_ESTADIA,
				ReservationStatus.CHECKIN_PENDIENTE.transit(ReservationStatus.EN_ESTADIA));
	}

	@Test
	void enEstadiaPermiteCheckout() {
		assertEquals(ReservationStatus.CHECKOUT, ReservationStatus.EN_ESTADIA.transit(ReservationStatus.CHECKOUT));
	}

	@Test
	void reglaDeNegocioBloqueaCheckinSinConfirmar() {
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CREADA.transit(ReservationStatus.CHECKIN_PENDIENTE));
	}

	@Test
	void reglaDeNegocioBloqueaEnEstadiaSinCheckin() {
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CONFIRMADA.transit(ReservationStatus.EN_ESTADIA));
	}

	@Test
	void transicionATrasDesdeCreadaQuedaBloqueada() {
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CREADA.transit(ReservationStatus.EN_ESTADIA));
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CREADA.transit(ReservationStatus.CHECKOUT));
	}

	@Test
	void checkoutEsTerminal() {
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CHECKOUT.transit(ReservationStatus.CONFIRMADA));
	}

	@Test
	void canceladaEsTerminal() {
		assertThrows(InvalidStatusTransitionException.class,
				() -> ReservationStatus.CANCELADA.transit(ReservationStatus.CREADA));
	}

	@Test
	void transicionAlMismoEstadoEsIdempotente() {
		assertEquals(ReservationStatus.CREADA, ReservationStatus.CREADA.transit(ReservationStatus.CREADA));
		assertEquals(ReservationStatus.CHECKOUT, ReservationStatus.CHECKOUT.transit(ReservationStatus.CHECKOUT));
	}
}